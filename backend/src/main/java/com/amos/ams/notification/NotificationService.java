package com.amos.ams.notification;

import com.amos.ams.domain.AppNotification;
import com.amos.ams.domain.User;
import com.amos.ams.kafka.EventPublisher;
import com.amos.ams.kafka.NotificationEvent;
import com.amos.ams.repository.NotificationRepository;
import com.amos.ams.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EventPublisher publisher;
    private final JavaMailSender mailSender;
    private final String mailFrom;

    public NotificationService(NotificationRepository notifications, UserRepository users,
                               EventPublisher publisher,
                               org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSender,
                               @Value("${spring.mail.username:}") String mailFrom) {
        this.notifications = notifications;
        this.users = users;
        this.publisher = publisher;
        this.mailSender = mailSender.getIfAvailable();
        this.mailFrom = mailFrom;
    }

    public void emit(List<Long> userIds, String title, String body, String type, String entityType, String entityId) {
        NotificationEvent event = new NotificationEvent();
        event.setUserIds(userIds);
        event.setTitle(title);
        event.setBody(body);
        event.setType(type);
        event.setChannels(List.of("IN_APP", "EMAIL"));
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        publisher.publishNotification(event);
        persistAndDeliver(event);
    }

    @Transactional
    public void persistAndDeliver(NotificationEvent event) {
        if (event.getUserIds() == null) return;
        List<String> channels = event.getChannels() == null ? List.of("IN_APP") : event.getChannels();
        for (Long userId : event.getUserIds()) {
            User user = users.findById(userId).orElse(null);
            if (user == null) continue;
            for (String channel : channels) {
                AppNotification n = new AppNotification();
                n.setUser(user);
                n.setTitle(event.getTitle());
                n.setBody(event.getBody());
                n.setType(event.getType());
                n.setChannel(channel);
                n.setEntityType(event.getEntityType());
                n.setEntityId(event.getEntityId());
                deliver(n, user);
                notifications.save(n);
            }
        }
    }

    private void deliver(AppNotification n, User user) {
        if ("EMAIL".equals(n.getChannel())) {
            try {
                if (mailSender != null && user.getEmail() != null) {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    if (mailFrom != null && !mailFrom.isBlank()) {
                        msg.setFrom(mailFrom);
                    }
                    msg.setTo(user.getEmail());
                    msg.setSubject(n.getTitle());
                    msg.setText(n.getBody());
                    mailSender.send(msg);
                } else {
                    log.info("EMAIL [{}] to {}: {}", n.getType(), user.getEmail(), n.getTitle());
                }
                n.setDeliveryStatus("SENT");
                n.setDeliveredAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Email delivery failed for {}: {}", user.getEmail(), ex.getMessage());
                n.setDeliveryStatus("FAILED");
            }
        } else if ("SMS".equals(n.getChannel())) {
            log.info("SMS [{}] to {}: {}", n.getType(), user.getPhone(), n.getTitle());
            n.setDeliveryStatus("SENT");
            n.setDeliveredAt(Instant.now());
        } else {
            n.setDeliveryStatus("SENT");
            n.setDeliveredAt(Instant.now());
        }
    }
}
