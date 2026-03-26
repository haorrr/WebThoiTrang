package com.fashionshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fashion Shop — Đặt lại mật khẩu");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildResetPasswordEmail(resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildResetPasswordEmail(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #f8f8f8; padding: 30px; border-radius: 8px;">
                        <h1 style="color: #1a1a1a; text-align: center;">Fashion Shop</h1>
                        <h2 style="color: #333;">Đặt lại mật khẩu</h2>
                        <p style="color: #555;">Bạn đã yêu cầu đặt lại mật khẩu. Nhấn vào nút bên dưới để tiếp tục:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s"
                               style="background: #1a1a1a; color: white; padding: 14px 32px;
                                      text-decoration: none; border-radius: 4px; font-size: 16px;">
                                Đặt lại mật khẩu
                            </a>
                        </div>
                        <p style="color: #888; font-size: 14px;">
                            Link này sẽ hết hạn sau <strong>15 phút</strong>.<br>
                            Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="color: #aaa; font-size: 12px; text-align: center;">
                            &copy; 2026 Fashion Shop. All rights reserved.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(resetLink);
    }
}
