package com.amos.ams.scheduler;

import com.amos.ams.domain.RecurringSchedule;
import com.amos.ams.notification.NotificationService;
import com.amos.ams.repository.RecurringScheduleRepository;
import com.amos.ams.repository.UserRepository;
import com.amos.ams.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class MaintenanceJobs {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceJobs.class);
    private final ActivityService activities;
    private final RecurringScheduleRepository schedules;
    private final UserRepository users;
    private final NotificationService notifications;

    public MaintenanceJobs(ActivityService activities, RecurringScheduleRepository schedules,
                           UserRepository users, NotificationService notifications) {
        this.activities = activities;
        this.schedules = schedules;
        this.users = users;
        this.notifications = notifications;
    }

    @Scheduled(fixedDelay = 300000)
    public void escalateAndRemind() {
        try {
            activities.escalateOverdue();
            int opened = activities.openDueSchedules();
            if (opened > 0) {
                log.info("Opened {} work orders from due schedules", opened);
            }
            Instant horizon = Instant.now().plus(2, ChronoUnit.DAYS);
            List<Long> planners = users.findAll().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r ->
                            List.of("MAINTENANCE_MANAGER", "SUPERVISOR", "ADMIN").contains(r.getCode())))
                    .map(u -> u.getId())
                    .toList();
            for (RecurringSchedule s : schedules.findByActiveTrue()) {
                if (s.getNextDueAt() != null && s.getNextDueAt().isBefore(horizon)) {
                    notifications.emit(planners,
                            "Upcoming: " + s.getAircraft().getRegistration() + " due check",
                            "Recurring maintenance is due by " + s.getNextDueAt(),
                            "DUE_REMINDER", "RecurringSchedule", String.valueOf(s.getId()));
                }
            }
        } catch (Exception ex) {
            log.warn("Scheduled maintenance job failed: {}", ex.getMessage());
        }
    }
}
