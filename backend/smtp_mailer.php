<?php
/**
 * TheStudent - Pure PHP Zero-Dependency SMTP Client
 * Supports SSL/TLS encryption for Gmail SMTP (smtp.gmail.com:465)
 */
class TheStudentSmtpMailer {
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

    public function getLastError(): string {
        return $this->lastError;
    }

    /**
     * Send an HTML email via SMTP
     */
    public function send(string $toEmail, string $subject, string $htmlBody, string $textAltBody = ''): bool {
        $errno = 0;
        $errstr = '';

        $context = stream_context_create([
            'ssl' => [
                'verify_peer' => false,
                'verify_peer_name' => false,
                'allow_self_signed' => true
            ]
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
            error_log($this->lastError);
            return false;
        }

        stream_set_timeout($socket, $this->timeout);

        $readResponse = function() use ($socket) {
            $response = '';
            while ($line = fgets($socket, 515)) {
                $response .= $line;
                // SMTP multi-line response check: 4th char is space when finished
                if (isset($line[3]) && $line[3] === ' ') {
                    break;
                }
            }
            return $response;
        };

        $sendCommand = function(string $cmd, array $expectedCodes) use ($socket, $readResponse) {
            fputs($socket, $cmd . "\r\n");
            $res = $readResponse();
            $code = substr($res, 0, 3);
            if (!in_array($code, $expectedCodes)) {
                throw new Exception("SMTP command [{$cmd}] failed with code {$code}: {$res}");
            }
            return $res;
        };

        try {
            // Read greeting (220)
            $greeting = $readResponse();
            if (substr($greeting, 0, 3) !== '220') {
                throw new Exception("Invalid greeting from server: {$greeting}");
            }

            // EHLO
            $sendCommand('EHLO ' . gethostname(), ['250']);

            // AUTH LOGIN
            $sendCommand('AUTH LOGIN', ['334']);
            $sendCommand(base64_encode($this->username), ['334']);
            $sendCommand(base64_encode($this->password), ['235']);

            // MAIL FROM
            $sendCommand('MAIL FROM:<' . $this->fromEmail . '>', ['250']);

            // RCPT TO
            $sendCommand('RCPT TO:<' . trim($toEmail) . '>', ['250', '251']);

            // DATA
            $sendCommand('DATA', ['354']);

            // Build MIME message
            $boundary = "bnd_" . md5(uniqid(microtime(), true));
            $plainText = !empty($textAltBody) ? $textAltBody : strip_tags(str_replace(['<br>', '<br/>', '<br />', '</p>'], "\n", $htmlBody));

            $headers = [];
            $headers[] = 'Date: ' . date('r');
            $headers[] = 'From: =?UTF-8?B?' . base64_encode($this->fromName) . '?= <' . $this->fromEmail . '>';
            $headers[] = 'To: <' . trim($toEmail) . '>';
            $headers[] = 'Subject: =?UTF-8?B?' . base64_encode($subject) . '?=';
            $headers[] = 'MIME-Version: 1.0';
            $headers[] = 'Content-Type: multipart/alternative; boundary="' . $boundary . '"';
            $headers[] = 'X-Mailer: TheStudent-SecureAuth/2.0';

            $body = implode("\r\n", $headers) . "\r\n\r\n";

            // Plain text part
            $body .= "--" . $boundary . "\r\n";
            $body .= "Content-Type: text/plain; charset=UTF-8\r\n";
            $body .= "Content-Transfer-Encoding: base64\r\n\r\n";
            $body .= chunk_split(base64_encode($plainText)) . "\r\n";

            // HTML part
            $body .= "--" . $boundary . "\r\n";
            $body .= "Content-Type: text/html; charset=UTF-8\r\n";
            $body .= "Content-Transfer-Encoding: base64\r\n\r\n";
            $body .= chunk_split(base64_encode($htmlBody)) . "\r\n";

            // End boundary
            $body .= "--" . $boundary . "--\r\n";

            // Send body + ending dot
            fputs($socket, $body . "\r\n.\r\n");
            $dataRes = $readResponse();
            if (substr($dataRes, 0, 3) !== '250') {
                throw new Exception("DATA rejected by server: {$dataRes}");
            }

            // QUIT
            $sendCommand('QUIT', ['221']);
            fclose($socket);
            return true;

        } catch (Exception $e) {
            $this->lastError = $e->getMessage();
            error_log("TheStudentSmtpMailer Exception: " . $e->getMessage());
            if (is_resource($socket)) {
                @fclose($socket);
            }
            return false;
        }
    }
}
