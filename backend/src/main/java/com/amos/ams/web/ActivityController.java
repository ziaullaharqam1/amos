package com.amos.ams.web;

import com.amos.ams.dto.Dtos;
import com.amos.ams.repository.NotificationRepository;
import com.amos.ams.security.CurrentUser;
import com.amos.ams.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityController {
    private final ActivityService activities;
    private final CurrentUser currentUser;
    private final NotificationRepository notificationRepository;

    public ActivityController(ActivityService activities, CurrentUser currentUser,
                              NotificationRepository notificationRepository) {
        this.activities = activities;
        this.currentUser = currentUser;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public Dtos.DashboardMetrics dashboard() {
        Dtos.DashboardMetrics metrics = activities.dashboard(currentUser.require());
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(currentUser.require().getId());
        return new Dtos.DashboardMetrics(metrics.openActivities(), metrics.inProgress(), metrics.overdue(),
                metrics.awaitingQa(), unread, metrics.dueSoon(), metrics.aog());
    }

    @GetMapping("/activities")
    @PreAuthorize("hasAuthority('ACTIVITY_VIEW')")
    public List<Dtos.ActivitySummary> list(@RequestParam(required = false) String state,
                                           @RequestParam(required = false) Long aircraftId) {
        return activities.list(state, aircraftId);
    }

    @GetMapping("/activities/queue")
    @PreAuthorize("hasAuthority('ACTIVITY_WORK')")
    public List<Dtos.ActivitySummary> queue() {
        return activities.workQueue(currentUser.require());
    }

    @GetMapping("/activities/{id}")
    @PreAuthorize("hasAuthority('ACTIVITY_VIEW')")
    public Dtos.ActivityDetail get(@PathVariable Long id) {
        return activities.get(id, currentUser.require());
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAuthority('ACTIVITY_CREATE')")
    public Dtos.ActivityDetail create(@Valid @RequestBody Dtos.ActivityCreate req) {
        return activities.create(req, currentUser.require());
    }

    @PutMapping("/activities/{id}")
    @PreAuthorize("hasAnyAuthority('ACTIVITY_CREATE','ACTIVITY_WORK','ACTIVITY_ASSIGN')")
    public Dtos.ActivityDetail update(@PathVariable Long id, @RequestBody Dtos.ActivityUpdate req) {
        return activities.update(id, req, currentUser.require());
    }

    @PostMapping("/activities/{id}/assign")
    @PreAuthorize("hasAuthority('ACTIVITY_ASSIGN')")
    public Dtos.ActivityDetail assign(@PathVariable Long id, @Valid @RequestBody Dtos.AssignRequest req) {
        return activities.assign(id, req, currentUser.require());
    }

    @PostMapping("/activities/{id}/transition")
    @PreAuthorize("isAuthenticated()")
    public Dtos.ActivityDetail transition(@PathVariable Long id, @Valid @RequestBody Dtos.TransitionRequest req) {
        return activities.transition(id, req, currentUser.require());
    }

    @PostMapping("/activities/{id}/logs")
    @PreAuthorize("hasAuthority('ACTIVITY_WORK')")
    public Dtos.ActivityDetail log(@PathVariable Long id, @RequestBody Dtos.LogRequest req) {
        return activities.addLog(id, req, currentUser.require());
    }

    @PostMapping("/schedules/{id}/generate")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public Dtos.ActivityDetail generate(@PathVariable Long id) {
        return activities.generateFromSchedule(id, currentUser.require());
    }

    @GetMapping(value = "/reports/compliance", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public String report() {
        return activities.complianceReport();
    }
}
