package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.NotificationDtos;
import fr.edc3.pmt.domain.enums.NotificationKind;
import fr.edc3.pmt.domain.repository.AccountRepository;
import fr.edc3.pmt.domain.model.UserNotification;
import fr.edc3.pmt.domain.repository.WorkItemRepository;
import fr.edc3.pmt.domain.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final AccountRepository accountRepository;
    private final WorkItemRepository workItemRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@pmt.local}")
    private String mailFrom;

    @Transactional
    public void createTaskAssignedNotification(Long accountId, Long workItemId) {
        String targetEmail = accountRepository.findById(accountId).map(a -> a.getEmail()).orElse(null);
        String workItemTitle = workItemRepository.findById(workItemId).map(w -> w.getTitle()).orElse("Task");

        boolean sent = false;
        LocalDateTime sentAt = null;

        if (targetEmail != null) {
            if (mailEnabled) {
                try {
                    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
                    if (mailSender == null) {
                        log.warn("SMTP_NOT_CONFIGURED mailEnabled=true but no JavaMailSender bean is available");
                    } else {
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setFrom(mailFrom);
                        message.setTo(targetEmail);
                        message.setSubject("Task assigned");
                        message.setText("You have been assigned: " + workItemTitle + " (#" + workItemId + ").");
                        mailSender.send(message);
                        sent = true;
                        sentAt = LocalDateTime.now();
                    }
                } catch (Exception ex) {
                    log.warn("SMTP_SEND_FAILED to={} workItemId={} cause={}", targetEmail, workItemId, ex.getMessage());
                }
            } else {
                log.info("EMAIL_NOTIFICATION_SIMULATED to={} subject='Task assigned' body='You have been assigned: {} (#{}).'",
                        targetEmail,
                        workItemTitle,
                        workItemId);
                sent = true;
                sentAt = LocalDateTime.now();
            }
        }

        UserNotification notification = UserNotification.builder()
                .accountId(accountId)
                .workItemId(workItemId)
                .kind(NotificationKind.WORK_ITEM_ASSIGNED)
                .isSent(sent)
                .sentAt(sentAt)
                .build();
        userNotificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> findByAccountId(Long accountId) {
        return userNotificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(n -> new NotificationDtos.NotificationResponse(
                        n.getId(),
                        n.getAccountId(),
                        n.getWorkItemId(),
                        n.getKind(),
                        n.getIsSent(),
                        n.getSentAt(),
                        n.getCreatedAt()
                ))
                .toList();
    }
}
