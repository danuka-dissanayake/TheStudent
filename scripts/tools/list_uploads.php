<?php
/**
 * Temporary diagnostic: what is actually inside the uploads folder?
 *
 * Upload this file to public_html/ (next to the `uploads` folder), open it as
 *
 *     https://app.quizontal.site/list_uploads.php?token=CHECK123
 *
 * and it prints the real paths, so a 404 can be traced to the exact mismatch
 * between the URL and the disk. DELETE IT when you are done — it only lists
 * file names (never file contents), but nothing on a live site should stay
 * open to a query string.
 */

declare(strict_types=1);

const TS_LIST_TOKEN = 'CHECK123';

if (($_GET['token'] ?? '') !== TS_LIST_TOKEN) {
    http_response_code(404);
    exit('Not found');
}

header('Content-Type: text/plain; charset=utf-8');

$docroot = rtrim((string) ($_SERVER['DOCUMENT_ROOT'] ?? ''), '/');
$root = $docroot . '/uploads/papers';

echo "DOCUMENT_ROOT : {$docroot}\n";
echo "SCRIPT        : {$_SERVER['SCRIPT_NAME']}\n";
echo "HOST          : {$_SERVER['HTTP_HOST']}\n";
echo "looking in    : {$root}\n";
echo 'exists        : ' . (is_dir($root) ? 'yes' : 'NO — this is the problem') . "\n";

if (!is_dir($root)) {
    // Show what IS under uploads, so the real nesting becomes obvious.
    $uploads = $docroot . '/uploads';
    echo "\ncontents of {$uploads}:\n";
    if (is_dir($uploads)) {
        foreach (scandir($uploads) as $entry) {
            if ($entry === '.' || $entry === '..') {
                continue;
            }
            echo '  ' . (is_dir("{$uploads}/{$entry}") ? '[dir] ' : '[file] ') . $entry . "\n";
        }
    } else {
        echo "  (no uploads folder at all)\n";
    }
    exit;
}

/** Print two levels: subject folders, then up to four files each. */
$folders = [];
foreach (scandir($root) as $entry) {
    if ($entry !== '.' && $entry !== '..' && is_dir("{$root}/{$entry}")) {
        $folders[] = $entry;
    }
}

echo "\nsubject folders (" . count($folders) . "):\n";
foreach (array_slice($folders, 0, 40) as $folder) {
    $files = array_values(array_filter(
        scandir("{$root}/{$folder}") ?: [],
        static fn($f) => $f !== '.' && $f !== '..' && !is_dir("{$root}/{$folder}/{$f}")
    ));
    echo '  ' . str_pad($folder, 34) . count($files) . " file(s)\n";
    foreach (array_slice($files, 0, 3) as $file) {
        echo "      {$file}\n";
    }
}
if (count($folders) > 40) {
    echo '  … and ' . (count($folders) - 40) . " more\n";
}

$total = 0;
foreach ($folders as $folder) {
    foreach (scandir("{$root}/{$folder}") ?: [] as $file) {
        if ($file !== '.' && $file !== '..' && is_file("{$root}/{$folder}/{$file}")) {
            $total++;
        }
    }
}
echo "\ntotal files: {$total}\n";
echo "sample URL : https://{$_SERVER['HTTP_HOST']}/uploads/papers/"
    . ($folders[0] ?? 'subject') . "/\n";
