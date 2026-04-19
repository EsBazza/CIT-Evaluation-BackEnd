package com.alonzo.citeval.service;

import com.alonzo.citeval.model.entity.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EvaluationConfirmationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationConfirmationEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean confirmationEnabled;
    private final String fromAddress;
    private final String mailHost;

    public EvaluationConfirmationEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                              @Value("${app.email.confirmation-enabled:false}") boolean confirmationEnabled,
                                              @Value("${app.email.confirmation-from:no-reply@citeval.local}") String fromAddress,
                                              @Value("${spring.mail.host:}") String mailHost) {
        this.mailSenderProvider = mailSenderProvider;
        this.confirmationEnabled = confirmationEnabled;
        this.fromAddress = fromAddress;
        this.mailHost = mailHost;
    }

    public void sendSubmissionConfirmation(Evaluation evaluation) {
        if (!confirmationEnabled || evaluation == null) {
            return;
        }

        String recipient = safe(evaluation.getStudentEmail());
        if (recipient.isBlank()) {
            return;
        }

        if (safe(mailHost).isBlank()) {
            logger.warn("Email confirmation is enabled but spring.mail.host is not configured. Skipping email.");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Email confirmation is enabled but JavaMailSender is unavailable. Skipping email.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setFrom(fromAddress);
        message.setSubject("Evaluation Submitted Successfully");
        message.setText(buildBody(evaluation));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            logger.warn("Failed to send confirmation email for evaluation {}", evaluation.getId(), ex);
        }
    }

    private String buildBody(Evaluation evaluation) {
        return "Hello,\n\n"
                + "Your evaluation has been submitted successfully.\n\n"
                + "Reference ID: " + safeId(evaluation.getId()) + "\n"
                + "Faculty: " + safe(evaluation.getFacultyEmail()) + "\n"
                + "Section: " + safe(evaluation.getSection()) + "\n\n"
                + "If you did not submit this, please contact the system administrator.\n\n"
                + "- CIT Eval";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeId(Long id) {
        return id == null ? "N/A" : id.toString();
    }
}
