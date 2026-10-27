<?php
/**
 * TheStudent - Zero-dependency SMTP client
 * ========================================
 *
 * Hardened for production use:
 *  - Full TLS certificate + hostname verification (no MITM of SMTP creds).
 *  - CRLF injection guards on address / subject / header values.
 *  - Never leaks OTP codes in the subject line (notification previews, mail
 *    server logs, shared-device banners).
 *  - Proper multipart/alternative message with a plain-text fallback.
 *  - Bounded connect/read timeouts and graceful failure (caller decides what
 *    to do when mail delivery fails).
 */

declare(strict_types=1);

class TheStudentSmtpMailer
{
    private string $host;
    private int $port;
    private string $username;
    private string $password;
    private string $fromEmail;
    private string $fromName;
    private int $timeout;
    private string $lastError = '';

    public function __construct(
        string $username,
        string $password,
        string $host = 'ssl://smtp.gmail.com',
        int $port = 465,
        string $fromName = 'TheStudent Security',
        int $timeout = 15
    ) {
        $this->host = $host;
        $this->port = $port;
        $this->username = $username;
        $this->password = $password;
        $this->fromEmail = $username;
        $this->fromName = $fromName;
        $this->timeout = $timeout;
    }

    public function getLastError(): string
    {
        return $this->lastError;
    }

    /**
     * Reject anything containing CR/LF so user-controlled values cannot inject
     * extra SMTP headers (Bcc:, Reply-To:, ...).
     */
    private function assertSafeHeaderValue(string $value, string $field): string
    {
        if (preg_match('/[\r\n\0]/', $value)) {
            throw new InvalidArgumentException("Illegal characters in $field");
        }
        return $value;
    }

    /**
     * Send an HTML email via SMTP. Always returns bool - never throws.
     */
    public function send(string $toEmail, string $subject, string $htmlBody, string $textAltBody = ''): bool
    {
        $this->lastError = '';

        try {
            $toEmail = $this->assertSafeHeaderValue(trim($toEmail), 'recipient');
            if (!filter_var($toEmail, FILTER_VALIDATE_EMAIL)) {
                throw new InvalidArgumentException('Invalid recipient address');
            }
            $subject = $this->assertSafeHeaderValue($subject, 'subject');
            $this->assertSafeHeaderValue($this->fromName, 'from name');
        } catch (Throwable $e) {
            $this->lastError = $e->getMessage();
            error_log('[TheStudent][SMTP] ' . $this->lastError);
            return false;
        }

        $errno = 0;
        $errstr = '';

        // Strict TLS verification. Gmail presents a valid certificate, so there
        // is no reason to accept self-signed or mismatched hosts.
        $context = stream_context_create([
            'ssl' => [
                'verify_peer'       => true,
                'verify_peer_name'  => true,
                'allow_self_signed' => false,
                'SNI_enabled'       => true,
                'peer_name'         => preg_replace('#^ssl://#', '', $this->host),
                'crypto_method'     => STREAM_CRYPTO_METHOD_TLS_CLIENT,
            ],
        ]);

        $socket = @stream_socket_client(
            $this->host . ':' . $this->port,
            $errno,
            $errstr,
            $this->timeout,
            STREAM_CLIENT_CONNECT,
            $context
        );

        if (!$socket) {
            $this->lastError = "Could not connect to SMTP server: {$errstr} ({$errno})";
            error_log('[TheStudent][SMTP] ' . $this->lastError);
            return false;
        }

        stream_set_timeout($socket, $this->timeout);

        $readResponse = function () use ($socket): string {
            $response = '';
            while (($line = fgets($socket, 515)) !== false) {
                $response .= $line;
                // SMTP multi-line responses: "250-..." continues, "250 " ends.
                if (isset($line[3]) && $line[3] === ' ') {
                    break;
                }
            }
            return $response;
        };

        $sendCommand = function (string $cmd, array $expectedCodes) use ($socket, $readResponse): string {
            fputs($socket, $cmd . "\r\n");
            $res = $readResponse();
            $code = substr($res, 0, 3);
            if (!in_array($code, $expectedCodes, true)) {
                // Never log the command itself: AUTH LOGIN credentials are sent
                // through this helper.
                $safeLabel = preg_match('/^AUTH|^[A-Za-z0-9+\/=]{8,}$/', $cmd) ? '[redacted]' : $cmd;
                throw new RuntimeException("SMTP command {$safeLabel} failed with code {$code}");
            }
            return $res;
        };

        try {
            $greeting = $readResponse();
            if (substr($greeting, 0, 3) !== '220') {
                throw new RuntimeException('Invalid SMTP greeting');
            }

            $sendCommand('EHLO ' . (gethostname() ?: 'thestudent.local'), ['250']);

            $sendCommand('AUTH LOGIN', ['334']);
            $sendCommand(base64_encode($this->username), ['334']);
            $sendCommand(base64_encode($this->password), ['235']);

            $sendCommand('MAIL FROM:<' . $this->fromEmail . '>', ['250']);
            $sendCommand('RCPT TO:<' . $toEmail . '>', ['250', '251']);
            $sendCommand('DATA', ['354']);

            $boundary = 'bnd_' . bin2hex(random_bytes(16));
            $plainText = $textAltBody !== ''
                ? $textAltBody
                : trim(html_entity_decode(strip_tags(str_replace(['<br>', '<br/>', '<br />', '</p>', '</div>'], "\n", $htmlBody)), ENT_QUOTES | ENT_HTML5, 'UTF-8'));

            $headers = [
                'Date: ' . date('r'),
                'From: =?UTF-8?B?' . base64_encode($this->fromName) . '?= <' . $this->fromEmail . '>',
                'To: <' . $toEmail . '>',
                'Subject: =?UTF-8?B?' . base64_encode($subject) . '?=',
                'Message-ID: <' . bin2hex(random_bytes(16)) . '@' . preg_replace('#^ssl://#', '', $this->host) . '>',
                'MIME-Version: 1.0',
                'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
                'X-Mailer: TheStudent-Auth/3.0',
                'Auto-Submitted: auto-generated',
                'X-Auto-Response-Suppress: All',
            ];

            $body = implode("\r\n", $headers) . "\r\n\r\n";

            $body .= '--' . $boundary . "\r\n";
            $body .= "Content-Type: text/plain; charset=UTF-8\r\n";
            $body .= "Content-Transfer-Encoding: base64\r\n\r\n";
            $body .= chunk_split(base64_encode($plainText)) . "\r\n";

            $body .= '--' . $boundary . "\r\n";
            $body .= "Content-Type: text/html; charset=UTF-8\r\n";
            $body .= "Content-Transfer-Encoding: base64\r\n\r\n";
            $body .= chunk_split(base64_encode($htmlBody)) . "\r\n";

            $body .= '--' . $boundary . "--\r\n";

            fputs($socket, $body . "\r\n.\r\n");
            $dataRes = $readResponse();
            if (substr($dataRes, 0, 3) !== '250') {
                throw new RuntimeException('SMTP DATA rejected with code ' . substr($dataRes, 0, 3));
            }

            $sendCommand('QUIT', ['221']);
            fclose($socket);
            return true;
        } catch (Throwable $e) {
            $this->lastError = $e->getMessage();
            error_log('[TheStudent][SMTP] ' . $this->lastError);
            if (is_resource($socket)) {
                @fclose($socket);
            }
            return false;
        }
    }
}
