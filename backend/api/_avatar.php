<?php
/**
 * TheStudent — Avatar upload pipeline (profile pictures)
 * =====================================================
 *
 * Shared by `api/auth.php` (`?action=complete_profile`). Everything an attacker
 * could try to send us is rejected here, in this order:
 *
 *   1. PHP's own upload error codes (partial upload, no file, INI size…)
 *   2. Size: must be > 0 and <= TS_AVATAR_MAX_BYTES (5 MB)
 *   3. Origin: `is_uploaded_file()` — only files PHP actually received
 *   4. Content: `getimagesize()` must succeed and the type must be JPEG, PNG or
 *      WEBP (an extension or a Content-Type header proves nothing)
 *   5. MIME: `finfo` must agree, when the extension is available
 *   6. Dimensions: 64 … 6000 px, and <= 24 megapixels (decompression bombs)
 *   7. Re-encode with GD → strips EXIF, ICC payloads and any appended PHP
 *      (`GIF89a;<?php … ?>` style polyglots die here), and normalises the format
 *   8. Random 32-hex filename inside `uploads/avatars/` — the client filename is
 *      never used, so `../../config.php` and `shell.php.jpg` are impossible
 *   9. The target directory denies script execution via its own `.htaccess`
 *
 * The result stored in the database is a RELATIVE path (`uploads/avatars/x.jpg`),
 * never a full URL, so the site can move hosts without corrupting avatars.
 */

declare(strict_types=1);

/** Hard ceiling on an uploaded avatar (5 MB). The app sends ~150 KB. */
const TS_AVATAR_MAX_BYTES = 5242880;

/** Longest edge kept after re-encoding. */
const TS_AVATAR_STORE_EDGE = 1024;

/** Reject tiny/huge source images before decoding them. */
const TS_AVATAR_MIN_EDGE = 64;
const TS_AVATAR_MAX_EDGE = 6000;
const TS_AVATAR_MAX_PIXELS = 24000000; // 24 MP

/** Only these image types are accepted. */
const TS_AVATAR_ALLOWED_TYPES = [IMAGETYPE_JPEG, IMAGETYPE_PNG, IMAGETYPE_WEBP];

/** Relative directory (from the backend root) where avatars live. */
const TS_AVATAR_RELATIVE_DIR = 'uploads/avatars/';

/**
 * Absolute path of the avatar directory, created on demand and protected with a
 * deny-execution `.htaccess`.
 */
function ts_avatar_dir(): string
{
    $dir = dirname(__DIR__) . '/' . TS_AVATAR_RELATIVE_DIR;

    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
    }
    if (is_dir($dir) && !is_file($dir . '.htaccess')) {
        // Same rules as backend/uploads/.htaccess: never execute anything here.
        // `php_flag` is avoided on purpose - it is a 500 error on FPM/CGI hosts.
        @file_put_contents(
            $dir . '.htaccess',
            "# TheStudent: avatars are images only - never execute scripts here.\n"
            . "<FilesMatch \"\\.(php|php[0-9]|phtml|pl|py|jsp|asp|sh|cgi|htaccess)$\">\n"
            . "    <IfModule mod_authz_core.c>\n"
            . "        Require all denied\n"
            . "    </IfModule>\n"
            . "    <IfModule !mod_authz_core.c>\n"
            . "        Order allow,deny\n"
            . "        Deny from all\n"
            . "    </IfModule>\n"
            . "</FilesMatch>\n"
            . "Options -Indexes\n"
            . "Header set X-Content-Type-Options \"nosniff\"\n"
        );
    }

    return $dir;
}

/** Human-readable message for a PHP upload error code. */
function ts_avatar_upload_error_message(int $code): string
{
    switch ($code) {
        case UPLOAD_ERR_INI_SIZE:
        case UPLOAD_ERR_FORM_SIZE:
            return 'That image is too large. Please choose one under 5 MB.';
        case UPLOAD_ERR_PARTIAL:
            return 'The upload was interrupted. Please try again.';
        case UPLOAD_ERR_NO_FILE:
            return 'No image was received.';
        case UPLOAD_ERR_NO_TMP_DIR:
        case UPLOAD_ERR_CANT_WRITE:
        case UPLOAD_ERR_EXTENSION:
            return 'The server could not accept the upload. Please try again later.';
        default:
            return 'The image could not be uploaded. Please try again.';
    }
}

/**
 * Validate, sanitise and store one uploaded avatar.
 *
 * @param array $file One entry of `$_FILES`.
 * @return string Relative path, e.g. `uploads/avatars/3f2a…c1.jpg`.
 * @throws RuntimeException With a message that is safe to show the student.
 */
function ts_store_avatar(array $file, int $userId): string
{
    // 1. PHP's upload status -------------------------------------------------
    $errorCode = (int) ($file['error'] ?? UPLOAD_ERR_NO_FILE);
    if ($errorCode !== UPLOAD_ERR_OK) {
        throw new RuntimeException(ts_avatar_upload_error_message($errorCode));
    }

    $tmpPath = (string) ($file['tmp_name'] ?? '');
    if ($tmpPath === '') {
        throw new RuntimeException('No image was received.');
    }

    // 2. Origin: only a genuine upload, never an arbitrary local path --------
    if (!is_uploaded_file($tmpPath)) {
        throw new RuntimeException('The image could not be verified. Please try again.');
    }

    // 3. Size ----------------------------------------------------------------
    $size = @filesize($tmpPath);
    if ($size === false || $size <= 0) {
        throw new RuntimeException('The uploaded file is empty.');
    }
    if ($size > TS_AVATAR_MAX_BYTES) {
        throw new RuntimeException('That image is too large. Please choose one under 5 MB.');
    }
    if ($size < 128) {
        throw new RuntimeException('That file does not look like an image.');
    }

    // 4. Real content: the only trustworthy source of truth ------------------
    $info = @getimagesize($tmpPath);
    if (!is_array($info) || empty($info[0]) || empty($info[1]) || empty($info[2])) {
        throw new RuntimeException('That file is not a valid image. Please choose a JPG or PNG photo.');
    }

    $width  = (int) $info[0];
    $height = (int) $info[1];
    $type   = (int) $info[2];

    if (!in_array($type, TS_AVATAR_ALLOWED_TYPES, true)) {
        throw new RuntimeException('Please use a JPG, PNG or WEBP photo.');
    }
    if ($width < TS_AVATAR_MIN_EDGE || $height < TS_AVATAR_MIN_EDGE) {
        throw new RuntimeException('That image is too small (minimum ' . TS_AVATAR_MIN_EDGE . '×' . TS_AVATAR_MIN_EDGE . ' pixels).');
    }
    if ($width > TS_AVATAR_MAX_EDGE || $height > TS_AVATAR_MAX_EDGE || ($width * $height) > TS_AVATAR_MAX_PIXELS) {
        throw new RuntimeException('That image is too large in resolution. Please choose a smaller photo.');
    }

    // 5. MIME cross-check (defence in depth; a wrong MIME alone is not fatal
    //    because the real check is getimagesize + re-encoding) ---------------
    if (function_exists('finfo_open')) {
        $finfo = @finfo_open(FILEINFO_MIME_TYPE);
        if ($finfo !== false) {
            $mime = (string) @finfo_file($finfo, $tmpPath);
            @finfo_close($finfo);
            $allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
            if ($mime !== '' && !in_array($mime, $allowedMimes, true)) {
                throw new RuntimeException('That file is not a valid image. Please choose a JPG or PNG photo.');
            }
        }
    }

    // 6. Store ---------------------------------------------------------------
    $dir = ts_avatar_dir();
    if (!is_dir($dir) || !is_writable($dir)) {
        error_log('[TheStudent][avatar] upload dir not writable: ' . $dir);
        throw new RuntimeException('Photo uploads are temporarily unavailable. Please try again later.');
    }

    // Random name: the client-supplied name is ignored completely.
    $baseName = bin2hex(random_bytes(16));

    $gdAvailable = function_exists('imagecreatefromstring') && function_exists('imagejpeg');
    if ($gdAvailable) {
        $target = $dir . $baseName . '.jpg';
        ts_avatar_reencode_jpeg($tmpPath, $target, $width, $height);
    } else {
        // No GD on this host: keep the bytes but force the extension to match
        // the type we detected (never the name the client sent).
        $extension = $type === IMAGETYPE_PNG ? 'png' : ($type === IMAGETYPE_WEBP ? 'webp' : 'jpg');
        $target = $dir . $baseName . '.' . $extension;
        if (!@move_uploaded_file($tmpPath, $target)) {
            error_log('[TheStudent][avatar] move_uploaded_file failed for user ' . $userId);
            throw new RuntimeException('The image could not be saved. Please try again.');
        }
        @chmod($target, 0644);
    }

    // PHP may still hold the temporary file when move_uploaded_file was used
    // above; unlink is harmless otherwise.
    @unlink($tmpPath);

    return TS_AVATAR_RELATIVE_DIR . basename($target);
}

/**
 * Decode → downscale → re-encode as JPEG.
 *
 * Re-encoding is the step that neutralises polyglot files: the output contains
 * only pixel data, so an appended `<?php … ?>` payload, EXIF script, or extra
 * ZIP/HTML content cannot survive.
 */
function ts_avatar_reencode_jpeg(string $sourcePath, string $targetPath, int $width, int $height): void
{
    $raw = @file_get_contents($sourcePath);
    if ($raw === false || $raw === '') {
        throw new RuntimeException('That file could not be read as an image.');
    }

    $image = @imagecreatefromstring($raw);
    if ($image === false) {
        throw new RuntimeException('That image could not be processed. Please try another photo.');
    }

    try {
        $maxEdge = TS_AVATAR_STORE_EDGE;
        $scale   = min(1.0, $maxEdge / max($width, $height));
        $newW    = max(1, (int) round($width * $scale));
        $newH    = max(1, (int) round($height * $scale));

        $canvas = imagecreatetruecolor($newW, $newH);
        if ($canvas === false) {
            throw new RuntimeException('That image could not be processed. Please try another photo.');
        }

        // Flatten transparency onto white so PNGs with alpha do not turn black.
        $white = imagecolorallocate($canvas, 255, 255, 255);
        imagefilledrectangle($canvas, 0, 0, $newW, $newH, $white);
        imagecopyresampled($canvas, $image, 0, 0, 0, 0, $newW, $newH, $width, $height);

        $saved = @imagejpeg($canvas, $targetPath, 88);
        imagedestroy($canvas);

        if (!$saved || !is_file($targetPath) || filesize($targetPath) === 0) {
            @unlink($targetPath);
            throw new RuntimeException('The image could not be saved. Please try again.');
        }
        @chmod($targetPath, 0644);
    } finally {
        imagedestroy($image);
    }
}

/**
 * Delete a previously stored avatar. Only touches files inside the avatar
 * directory that match our own random-name pattern, so a crafted
 * `profile_picture` value can never delete anything else on the server.
 */
function ts_avatar_delete(?string $relativePath): void
{
    if ($relativePath === null || $relativePath === '') {
        return;
    }
    if (strpos($relativePath, TS_AVATAR_RELATIVE_DIR) !== 0) {
        return; // Google photo URL or something we did not create.
    }

    $name = basename($relativePath);
    if (!preg_match('/^[a-f0-9]{32}\.(jpg|png|webp)$/', $name)) {
        return;
    }

    $path = dirname(__DIR__) . '/' . TS_AVATAR_RELATIVE_DIR . $name;
    if (is_file($path)) {
        @unlink($path);
    }
}

/**
 * Turn a stored relative path into an absolute URL the app can load.
 * Absolute (Google) URLs are returned untouched.
 */
function ts_avatar_public_url(?string $storedPath): ?string
{
    if ($storedPath === null || $storedPath === '') {
        return null;
    }
    if (preg_match('#^https?://#i', $storedPath)) {
        return $storedPath;
    }

    $scheme = (!empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off')
        || (int) ($_SERVER['SERVER_PORT'] ?? 0) === 443
        || strtolower((string) ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '')) === 'https'
        ? 'https'
        : 'http';

    $host = (string) ($_SERVER['HTTP_HOST'] ?? '');
    if ($host === '' || !preg_match('/^[A-Za-z0-9\.\-]+(:[0-9]{1,5})?$/', $host)) {
        return null; // Never echo an attacker-controlled Host header into a URL.
    }

    // backend/api/_avatar.php -> web root is two levels up.
    $basePath = str_replace('\\', '/', dirname(dirname((string) ($_SERVER['SCRIPT_NAME'] ?? ''))));
    $basePath = rtrim($basePath, '/');

    return $scheme . '://' . $host . $basePath . '/' . ltrim($storedPath, '/');
}
