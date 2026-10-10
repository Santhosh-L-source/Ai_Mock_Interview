package com.ai.interviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendStudentCredentials(String toEmail, String studentName, String rollNo,
                                        String password, String department, String studentYear) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("tamilselvans2468@gmail.com", "AI Interviewer");
            helper.setTo(toEmail);
            helper.setSubject("Your AI Interviewer Login Credentials");

            String html = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #4f46e5; color: white; padding: 24px; border-radius: 8px 8px 0 0; text-align: center;">
                        <h1 style="margin: 0; font-size: 22px;">AI Interviewer</h1>
                        <p style="margin: 8px 0 0; opacity: 0.9;">Your account has been created</p>
                    </div>
                    <div style="background: #ffffff; border: 1px solid #e2e8f0; border-top: none; padding: 24px; border-radius: 0 0 8px 8px;">
                        <p style="color: #1e293b; font-size: 15px;">Hello <strong>%s</strong>,</p>
                        <p style="color: #475569; font-size: 14px;">Your student account for the AI Interviewer platform has been created. Use the credentials below to log in:</p>
                        <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px; margin: 16px 0;">
                            <table style="width: 100%%; font-size: 14px; color: #334155;">
                                <tr><td style="padding: 4px 0; font-weight: 600;">Name:</td><td>%s</td></tr>
                                <tr><td style="padding: 4px 0; font-weight: 600;">Roll No:</td><td>%s</td></tr>
                                <tr><td style="padding: 4px 0; font-weight: 600;">Department:</td><td>%s</td></tr>
                                <tr><td style="padding: 4px 0; font-weight: 600;">Year:</td><td>%s</td></tr>
                                <tr><td style="padding: 4px 0; font-weight: 600;">Email (Login):</td><td style="color: #4f46e5; font-weight: 600;">%s</td></tr>
                                <tr><td style="padding: 4px 0; font-weight: 600;">Password:</td><td style="color: #4f46e5; font-weight: 600;">%s</td></tr>
                            </table>
                        </div>
                        <p style="color: #64748b; font-size: 13px; margin-top: 16px;">Please keep your credentials safe. You can change your password after logging in.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="color: #94a3b8; font-size: 12px; text-align: center;">This is an automated message from AI Interviewer. Do not reply to this email.</p>
                    </div>
                </div>
                """.formatted(studentName, studentName, rollNo != null ? rollNo : "N/A",
                    department, studentYear, toEmail, password);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Credentials email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", toEmail, e.getMessage());
        }
    }
}
