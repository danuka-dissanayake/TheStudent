package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Native Android SMTP client for sending branded OTP verification emails via Gmail SMTP.
 * Direct, zero-dependency, ultra-fast delivery.
 */
object TheStudentEmailSender {
    private const val TAG = "TheStudentEmailSender"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 465
    private const val SMTP_USER = "yourownwpbot@gmail.com"
    private const val SMTP_PASS = "tllmuempbiotaltb"
    private const val SENDER_NAME = "TheStudent Security"

    /**
     * Sends a 6-digit OTP verification code to the target student email.
     */
    suspend fun sendOtpEmail(
        recipientEmail: String,
        otpCode: String,
        reason: String = "registration"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val title = when (reason) {
                "ip_change" -> "Security Alert: New Sign-In Location"
                "unverified_account" -> "Account Verification Required"
                else -> "Email Verification"
            }
            val subtitle = when (reason) {
                "ip_change" -> "We detected a sign-in attempt from a new IP or device. If this was you, enter the code below to authorize access:"
                "unverified_account" -> "Your TheStudent account requires email verification. Enter the 6-digit code below to continue:"
                else -> "Use the 6-digit code below to securely verify your TheStudent account:"
            }

            val htmlBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                        .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                        .header { background: #047857; padding: 28px 24px; text-align: center; }
                        .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }
                        .header p { margin: 6px 0 0 0; color: #a7f3d0; font-size: 13px; }
                        .content { padding: 32px 28px; text-align: center; }
                        .title { font-size: 20px; font-weight: 700; color: #0f172a; margin-bottom: 10px; }
                        .subtitle { font-size: 14px; line-height: 1.6; color: #475569; margin-bottom: 24px; }
                        .code-box { background: #ecfdf5; border: 2px dashed #047857; border-radius: 14px; padding: 18px 24px; display: inline-block; margin-bottom: 24px; }
                        .code-text { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #047857; font-family: monospace, sans-serif; }
                        .expiry { font-size: 13px; color: #64748b; margin-bottom: 24px; }
                        .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>TheStudent</h1>
                            <p>Sri Lanka Education & AI Study Suite</p>
                        </div>
                        <div class="content">
                            <div class="title">$title</div>
                            <div class="subtitle">$subtitle</div>
                            <div class="code-box">
                                <div class="code-text">$otpCode</div>
                            </div>
                            <div class="expiry">⏰ This code will expire in <strong>10 minutes</strong>.<br>If you did not request this code, please ignore this email.</div>
                        </div>
                        <div class="footer">
                            🛡️ 100% Free & Secure for Sri Lankan Students 🇱🇰<br>
                            © ${SimpleDateFormat("yyyy", Locale.US).format(Date())} TheStudent. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket(SMTP_HOST, SMTP_PORT) as SSLSocket
            socket.soTimeout = 12000

            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), true)

            fun readLine(): String {
                val line = reader.readLine() ?: ""
                Log.d(TAG, "S: $line")
                return line
            }

            fun sendCmd(cmd: String) {
                Log.d(TAG, "C: $cmd")
                writer.print(cmd + "\r\n")
                writer.flush()
            }

            // 1. Read greeting
            readLine()

            // 2. EHLO
            sendCmd("EHLO localhost")
            while (true) {
                val line = readLine()
                if (line.length >= 4 && line[3] == ' ') break
            }

            // 3. AUTH LOGIN
            sendCmd("AUTH LOGIN")
            readLine()
            val userB64 = Base64.encodeToString(SMTP_USER.toByteArray(), Base64.NO_WRAP)
            sendCmd(userB64)
            readLine()
            val passB64 = Base64.encodeToString(SMTP_PASS.toByteArray(), Base64.NO_WRAP)
            sendCmd(passB64)
            val authResp = readLine()
            if (!authResp.startsWith("235")) {
                socket.close()
                return@withContext Result.failure(Exception("SMTP Authentication failed: $authResp"))
            }

            // 4. MAIL FROM & RCPT TO
            sendCmd("MAIL FROM:<$SMTP_USER>")
            readLine()
            sendCmd("RCPT TO:<${recipientEmail.trim()}>")
            readLine()

            // 5. DATA
            sendCmd("DATA")
            readLine()

            val subject = "Your TheStudent Verification Code: $otpCode"
            val subjectB64 = Base64.encodeToString(subject.toByteArray(), Base64.NO_WRAP)
            val nameB64 = Base64.encodeToString(SENDER_NAME.toByteArray(), Base64.NO_WRAP)

            val emailData = buildString {
                append("From: =?UTF-8?B?$nameB64?= <$SMTP_USER>\r\n")
                append("To: <${recipientEmail.trim()}>\r\n")
                append("Subject: =?UTF-8?B?$subjectB64?=\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Date: ${SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(Date())}\r\n")
                append("\r\n")
                append(htmlBody)
                append("\r\n.\r\n")
            }

            writer.print(emailData)
            writer.flush()
            val dataResp = readLine()

            sendCmd("QUIT")
            try { socket.close() } catch (_: Exception) {}

            if (dataResp.startsWith("250")) {
                Log.i(TAG, "OTP Email dispatched successfully to $recipientEmail")
                Result.success(Unit)
            } else {
                Result.failure(Exception("SMTP DATA failed: $dataResp"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending OTP email: ${e.message}", e)
            Result.failure(e)
        }
    }
}
