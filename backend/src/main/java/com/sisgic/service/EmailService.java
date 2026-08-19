package com.sisgic.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String mailFrom;

    @Value("${app.mail.from-name:}")
    private String mailFromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Autowired
    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public boolean isConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank();
    }

    public void sendPlainText(String to, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Mail is not configured; skipping email to recipient (subject='{}')", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (StringUtils.hasText(mailFromName)) {
                helper.setFrom(mailFrom, mailFromName);
            } else {
                helper.setFrom(mailFrom);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email sent successfully (subject='{}')", subject);
        } catch (Exception e) {
            log.error("Failed to send email (subject='{}'): {}", subject, e.getMessage());
            throw new IllegalStateException("Failed to send email", e);
        }
    }
}
