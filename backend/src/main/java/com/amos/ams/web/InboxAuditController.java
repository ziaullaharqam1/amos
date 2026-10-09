package com.amos.ams.web;

import com.amos.ams.domain.AppNotification;
import com.amos.ams.domain.AuditLog;
import com.amos.ams.dto.Dtos;
import com.amos.ams.exception.ApiException;
import com.amos.ams.repository.AuditLogRepository;
import com.amos.ams.repository.NotificationRepository;
import com.amos.ams.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InboxAuditController {
    private final NotificationRepository notifications;
    private final AuditLogRepository auditLogs;
    private final CurrentUser currentUser;

    public InboxAuditController(NotificationRepository notifications, AuditLogRepository auditLogs, CurrentUser currentUser) {
        this.notifications = notifications;
        this.auditLogs = auditLogs;
        this.currentUser = currentUser;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public List<Dtos.NotificationView> inbox() {
        return notifications.findByUserIdOrderByCreatedAtDesc(currentUser.require().getId()).stream()
                .filter(n -> "IN_APP".equals(n.getChannel()))
                .map(this::view)
                .toList();
    }

    @PostMapping("/notifications/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public Dtos.NotificationView markRead(@PathVariable Long id) {
        AppNotification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(currentUser.require().getId())) {
            throw ApiException.forbidden("Cannot access another user's notifications");
        }
        n.setReadAt(Instant.now());
        notifications.save(n);
        return view(n);
    }

    @PostMapping("/notifications/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public void readAll() {
        Long userId = currentUser.require().getId();
        notifications.findByUserIdOrderByCreatedAtDesc(userId).forEach(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(Instant.now());
                notifications.save(n);
            }
        });
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public Page<Dtos.AuditView> audit(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size,
                                      @RequestParam(required = false) String entityType,
                                      @RequestParam(required = false) String action) {
        Specification<AuditLog> spec = Specification.where(null);
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        return auditLogs.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(a -> new Dtos.AuditView(a.getId(), a.getActorUsername(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getBeforeValue(), a.getAfterValue(), a.getCreatedAt()));
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP");
    }

    private Dtos.NotificationView view(AppNotification n) {
        return new Dtos.NotificationView(n.getId(), n.getTitle(), n.getBody(), n.getType(), n.getChannel(),
                n.getEntityType(), n.getEntityId(), n.getReadAt(), n.getDeliveryStatus(), n.getCreatedAt());
    }
}
