package com.finsaathi.SipCalculator.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.sender}") // Injects sender email from application.properties
    private String fromEmail;

    /**
     * Sends an email with an attachment.
     * @param toEmail The recipient's email address.
     * @param subject The email subject.
     * @param body The email body (can be HTML).
     * @param attachmentBytes The PDF content as byte array.
     * @param attachmentFileName The desired file name for the attachment.
     */
    public void sendEmailWithAttachment(String toEmail, String subject, String body, byte[] attachmentBytes, String attachmentFileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true enables multipart message, which is required for attachments
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates that the body is HTML content

            if (attachmentBytes != null && attachmentBytes.length > 0) {
                // Add the attachment using a ByteArrayDataSource for in-memory bytes
                helper.addAttachment(attachmentFileName, new jakarta.mail.util.ByteArrayDataSource(attachmentBytes, "application/pdf"));
            }

            mailSender.send(message);
            logger.info("Email sent successfully to: " + toEmail + " with attachment: " + attachmentFileName);
        } catch (Exception e) {
            logger.severe("Failed to send email to " + toEmail + " with attachment " + attachmentFileName + ": " + e.getMessage());
            // In a production application, you might throw a custom EmailServiceException
            // or return a status to the calling method for error handling.
        }
    }
}
