package com.nearly.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from.name}")
    private String fromName;

    @Value("${email.from.address}")
    private String fromAddress;

    @Async
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            
            String subject;
            String htmlContent;
            
            if ("PASSWORD_RESET".equals(purpose)) {
                subject = "Reset Your Nearly Password";
                htmlContent = getPasswordResetEmailTemplate(otp);
            } else {
                subject = "Verify Your Nearly Email";
                htmlContent = getEmailVerificationTemplate(otp);
            }
            
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Nearly! 🎉");
            helper.setText(getWelcomeEmailTemplate(name), true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String getPasswordResetEmailTemplate(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 20px 20px 0 0; padding: 40px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">🔐 Password Reset</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0;">Nearly App</p>
                    </div>
                    <div style="background: white; border-radius: 0 0 20px 20px; padding: 40px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                        <p style="color: #333; font-size: 16px; line-height: 1.6;">
                            You requested to reset your password. Use the verification code below to proceed:
                        </p>
                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 12px; padding: 30px; text-align: center; margin: 30px 0;">
                            <span style="color: white; font-size: 36px; font-weight: bold; letter-spacing: 8px;">%s</span>
                        </div>
                        <p style="color: #666; font-size: 14px; text-align: center;">
                            ⏰ This code expires in <strong>10 minutes</strong>
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px; text-align: center;">
                            If you didn't request this, please ignore this email or contact support.<br>
                            Your password won't be changed unless you use this code.
                        </p>
                    </div>
                    <p style="color: #999; font-size: 12px; text-align: center; margin-top: 20px;">
                        © 2026 Nearly App. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(otp);
    }

    private String getEmailVerificationTemplate(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                    <div style="background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); border-radius: 20px 20px 0 0; padding: 40px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">✉️ Verify Your Email</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0;">Nearly App</p>
                    </div>
                    <div style="background: white; border-radius: 0 0 20px 20px; padding: 40px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                        <p style="color: #333; font-size: 16px; line-height: 1.6;">
                            Thanks for signing up! Please verify your email address using the code below:
                        </p>
                        <div style="background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); border-radius: 12px; padding: 30px; text-align: center; margin: 30px 0;">
                            <span style="color: white; font-size: 36px; font-weight: bold; letter-spacing: 8px;">%s</span>
                        </div>
                        <p style="color: #666; font-size: 14px; text-align: center;">
                            ⏰ This code expires in <strong>10 minutes</strong>
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px; text-align: center;">
                            If you didn't create an account, please ignore this email.
                        </p>
                    </div>
                    <p style="color: #999; font-size: 12px; text-align: center; margin-top: 20px;">
                        © 2026 Nearly App. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(otp);
    }

    private String getWelcomeEmailTemplate(String name) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 20px 20px 0 0; padding: 40px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">🎉 Welcome to Nearly!</h1>
                    </div>
                    <div style="background: white; border-radius: 0 0 20px 20px; padding: 40px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                        <p style="color: #333; font-size: 18px; line-height: 1.6;">
                            Hi <strong>%s</strong>,
                        </p>
                        <p style="color: #333; font-size: 16px; line-height: 1.6;">
                            Welcome to Nearly! We're excited to have you join our community.
                        </p>
                        <p style="color: #333; font-size: 16px; line-height: 1.6;">
                            Here's what you can do:
                        </p>
                        <ul style="color: #555; font-size: 14px; line-height: 2;">
                            <li>🌍 Discover activities and events nearby</li>
                            <li>👥 Join groups and meet like-minded people</li>
                            <li>📸 Share moments with friends</li>
                            <li>💬 Connect with people around you</li>
                        </ul>
                        <div style="text-align: center; margin-top: 30px;">
                            <a href="https://nearly.app" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 30px; font-weight: bold; display: inline-block;">
                                Get Started
                            </a>
                        </div>
                    </div>
                    <p style="color: #999; font-size: 12px; text-align: center; margin-top: 20px;">
                        © 2026 Nearly App. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(name);
    }
}

