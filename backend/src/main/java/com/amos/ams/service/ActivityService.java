package com.amos.ams.service;

import com.amos.ams.audit.AuditService;
import com.amos.ams.domain.*;
import com.amos.ams.dto.Dtos;
import com.amos.ams.exception.ApiException;
import com.amos.ams.notification.NotificationService;
import com.amos.ams.repository.*;
import com.amos.ams.workflow.WorkflowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActivityService {
    private final MaintenanceActivityRepository activities;
    private final AircraftRepository aircraft;
    private final CheckTypeRepository checkTypes;
    private final MaintenanceTaskRepository tasks;
    private final ComponentRepository components;
    private final RecurringScheduleRepository schedules;
    private final UserRepository users;
    private final ActivityAssignmentRepository assignments;
    private final ActivityLogRepository logs;
    private final WorkflowTransitionRepository transitions;
    private final AuditService audit;
    private final NotificationService notifications;

    public ActivityService(MaintenanceActivityRepository activities, AircraftRepository aircraft,
                           CheckTypeRepository checkTypes, MaintenanceTaskRepository tasks,
                           ComponentRepository components, RecurringScheduleRepository schedules,
                           UserRepository users, ActivityAssignmentRepository assignments,
                           ActivityLogRepository logs, WorkflowTransitionRepository transitions,
                           AuditService audit, NotificationService notifications) {
        this.activities = activities;
        this.aircraft = aircraft;
        this.checkTypes = checkTypes;
        this.tasks = tasks;
        this.components = components;
        this.schedules = schedules;
        this.users = users;
        this.assignments = assignments;
        this.logs = logs;
        this.transitions = transitions;
        this.audit = audit;
        this.notifications = notifications;
    }

    public List<Dtos.ActivitySummary> list(String state, Long aircraftId) {
        List<MaintenanceActivity> all = activities.findAll();
        return all.stream()
                .filter(a -> state == null || state.isBlank() || a.getState().equals(state))
                .filter(a -> aircraftId == null || a.getAircraft().getId().equals(aircraftId))
                .map(this::summary)
                .toList();
    }

    public List<Dtos.ActivitySummary> workQueue(User user) {
        return activities.findByAssignedToIdOrderByDueAtAsc(user.getId()).stream()
                .filter(a -> !List.of("VERIFIED", "CANCELLED").contains(a.getState()))
                .map(this::summary)
                .toList();
    }

    public Dtos.ActivityDetail get(Long id, User actor) {
        return detail(load(id), actor);
    }

    @Transactional
    public Dtos.ActivityDetail create(Dtos.ActivityCreate req, User actor) {
        MaintenanceActivity a = new MaintenanceActivity();
        a.setActivityNumber(nextNumber());
        a.setTitle(req.title());
        a.setDescription(req.description());
        a.setAircraft(aircraft.findById(req.aircraftId()).orElseThrow(() -> ApiException.badRequest("Unknown aircraft")));
        a.setCheckType(req.checkTypeId() == null ? null : checkTypes.findById(req.checkTypeId()).orElse(null));
        a.setTask(req.taskId() == null ? null : tasks.findById(req.taskId()).orElse(null));
        a.setComponent(req.componentId() == null ? null : components.findById(req.componentId()).orElse(null));
        a.setParent(req.parentId() == null ? null : load(req.parentId()));
        a.setSchedule(req.scheduleId() == null ? null : schedules.findById(req.scheduleId()).orElse(null));
        a.setPriority(req.priority() == null ? "NORMAL" : req.priority());
        a.setDueAt(req.dueAt());
        a.setPlannedStart(req.plannedStart());
        a.setPlannedEnd(req.plannedEnd());
        a.setStation(req.station());
        a.setRequestedBy(actor);
        a.setState(WorkflowEngine.PENDING);
        activities.save(a);
        audit.record(actor, "CREATE", "MaintenanceActivity", a.getId(), null, summary(a));
        notifyManagers("New maintenance request " + a.getActivityNumber(),
                actor.getFullName() + " opened " + a.getTitle() + " on " + a.getAircraft().getRegistration(),
                "ACTIVITY_CREATED", a);
        return detail(a, actor);
    }

    @Transactional
    public Dtos.ActivityDetail update(Long id, Dtos.ActivityUpdate req, User actor) {
        MaintenanceActivity a = load(id);
        Dtos.ActivitySummary before = summary(a);
        if (req.title() != null) a.setTitle(req.title());
        if (req.description() != null) a.setDescription(req.description());
        if (req.priority() != null) a.setPriority(req.priority());
        if (req.dueAt() != null) a.setDueAt(req.dueAt());
        if (req.plannedStart() != null) a.setPlannedStart(req.plannedStart());
        if (req.plannedEnd() != null) a.setPlannedEnd(req.plannedEnd());
        if (req.station() != null) a.setStation(req.station());
        if (req.findings() != null) a.setFindings(req.findings());
        if (req.actionsTaken() != null) a.setActionsTaken(req.actionsTaken());
        activities.save(a);
        audit.record(actor, "UPDATE", "MaintenanceActivity", id, before, summary(a));
        return detail(a, actor);
    }

    @Transactional
    public Dtos.ActivityDetail assign(Long id, Dtos.AssignRequest req, User actor) {
        MaintenanceActivity a = load(id);
        User assignee = users.findById(req.userId()).orElseThrow(() -> ApiException.badRequest("Unknown user"));
        String before = a.getAssignedTo() == null ? null : a.getAssignedTo().getUsername();
        a.setAssignedTo(assignee);
        if (WorkflowEngine.PENDING.equals(a.getState()) || WorkflowEngine.ESCALATED.equals(a.getState())) {
            applyTransition(a, WorkflowEngine.ASSIGNED, actor, "Assigned to " + assignee.getFullName());
        }
        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setActivity(a);
        assignment.setUser(assignee);
        assignment.setRoleOnJob(req.roleOnJob() == null ? "TECHNICIAN" : req.roleOnJob());
        assignment.setAssignedBy(actor);
        assignments.save(assignment);
        activities.save(a);
        audit.record(actor, "ASSIGN", "MaintenanceActivity", id, before, assignee.getUsername());
        notifications.emit(List.of(assignee.getId()),
                "Assigned: " + a.getActivityNumber(),
                "You were assigned " + a.getTitle() + " (" + a.getAircraft().getRegistration() + "). Due: " + a.getDueAt(),
                "TASK_ASSIGNED", "MaintenanceActivity", String.valueOf(a.getId()));
        return detail(a, actor);
    }

    @Transactional
    public Dtos.ActivityDetail transition(Long id, Dtos.TransitionRequest req, User actor) {
        MaintenanceActivity a = load(id);
        String from = a.getState();
        applyTransition(a, req.toState(), actor, req.comment());
        if (WorkflowEngine.IN_PROGRESS.equals(req.toState()) && a.getActualStart() == null) {
            a.setActualStart(Instant.now());
        }
        if (WorkflowEngine.COMPLETED.equals(req.toState())) {
            a.setActualEnd(Instant.now());
            notifyQaAndManagers("Completion submitted " + a.getActivityNumber(),
                    a.getTitle() + " is ready for verification", "APPROVAL_REQUEST", a);
        }
        if (WorkflowEngine.VERIFIED.equals(req.toState())) {
            a.setVerifiedBy(actor);
            rollSchedule(a);
        }
        if (WorkflowEngine.REJECTED.equals(req.toState()) && a.getAssignedTo() != null) {
            notifications.emit(List.of(a.getAssignedTo().getId()),
                    "Returned: " + a.getActivityNumber(),
                    req.comment() == null ? "Work was returned for rework" : req.comment(),
                    "ACTIVITY_REJECTED", "MaintenanceActivity", String.valueOf(a.getId()));
        }
        activities.save(a);
        audit.record(actor, "APPROVE".equals(req.toState()) || WorkflowEngine.VERIFIED.equals(req.toState()) ? "APPROVE" : "UPDATE",
                "MaintenanceActivity", id, from, req.toState());
        notifyWatchers(a, "Status: " + a.getActivityNumber() + " is now " + req.toState(),
                actor.getFullName() + " moved the job from " + from + " to " + req.toState(),
                "WORKFLOW_CHANGE");
        return detail(a, actor);
    }

    @Transactional
    public Dtos.ActivityDetail addLog(Long id, Dtos.LogRequest req, User actor) {
        MaintenanceActivity a = load(id);
        ActivityLog log = new ActivityLog();
        log.setActivity(a);
        log.setAuthor(actor);
        log.setLogType(req.logType() == null ? "PROGRESS" : req.logType());
        log.setFindings(req.findings());
        log.setActionsTaken(req.actionsTaken());
        log.setHoursSpent(req.hoursSpent());
        logs.save(log);
        if (req.findings() != null) a.setFindings(req.findings());
        if (req.actionsTaken() != null) a.setActionsTaken(req.actionsTaken());
        if (WorkflowEngine.ASSIGNED.equals(a.getState())) {
            applyTransition(a, WorkflowEngine.IN_PROGRESS, actor, "Work logged");
            a.setActualStart(Instant.now());
        }
        activities.save(a);
        audit.record(actor, "UPDATE", "ActivityLog", log.getId(), null, req);
        return detail(a, actor);
    }

    @Transactional
    public Dtos.ActivityDetail generateFromSchedule(Long scheduleId, User actor) {
        RecurringSchedule schedule = schedules.findById(scheduleId)
                .orElseThrow(() -> ApiException.notFound("Schedule not found"));
        if (activities.existsByScheduleIdAndStateNotIn(scheduleId,
                List.of(WorkflowEngine.VERIFIED, WorkflowEngine.CANCELLED))) {
            throw ApiException.conflict("An open work order already exists for this schedule");
        }
        String taskTitle = schedule.getTask() != null ? schedule.getTask().getTitle() : "Scheduled check";
        String checkCode = schedule.getCheckType() != null ? schedule.getCheckType().getCode() : "CHECK";
        Dtos.ActivityCreate req = new Dtos.ActivityCreate(
                schedule.getAircraft().getRegistration() + " " + checkCode + " — " + taskTitle,
                "Opened from recurring schedule #" + schedule.getId(),
                schedule.getAircraft().getId(),
                schedule.getCheckType() != null ? schedule.getCheckType().getId() : null,
                schedule.getTask() != null ? schedule.getTask().getId() : null,
                null, null, schedule.getId(),
                "HIGH",
                schedule.getNextDueAt(),
                null, null,
                schedule.getAircraft().getBaseStation()
        );
        return create(req, actor);
    }

    @Transactional
    public int openDueSchedules() {
        User planner = users.findByUsername("manager").or(() -> users.findByUsername("admin")).orElse(null);
        if (planner == null) return 0;
        Instant horizon = Instant.now().plus(2, ChronoUnit.DAYS);
        int opened = 0;
        for (RecurringSchedule schedule : schedules.findByActiveTrue()) {
            if (schedule.getNextDueAt() == null || schedule.getNextDueAt().isAfter(horizon)) continue;
            if (activities.existsByScheduleIdAndStateNotIn(schedule.getId(),
                    List.of(WorkflowEngine.VERIFIED, WorkflowEngine.CANCELLED))) {
                continue;
            }
            generateFromSchedule(schedule.getId(), planner);
            opened++;
        }
        return opened;
    }

    private void rollSchedule(MaintenanceActivity a) {
        RecurringSchedule schedule = a.getSchedule();
        if (schedule == null) return;
        Aircraft ac = a.getAircraft();
        schedule.setLastPerformedAt(Instant.now());
        schedule.setLastHours(ac.getTotalFlightHours());
        schedule.setLastCycles(ac.getTotalCycles());
        if (schedule.getIntervalDays() != null) {
            schedule.setNextDueAt(Instant.now().plus(schedule.getIntervalDays(), ChronoUnit.DAYS));
        }
        if (schedule.getIntervalHours() != null && ac.getTotalFlightHours() != null) {
            schedule.setNextDueHours(ac.getTotalFlightHours().add(schedule.getIntervalHours()));
        }
        if (schedule.getIntervalCycles() != null && ac.getTotalCycles() != null) {
            schedule.setNextDueCycles(ac.getTotalCycles() + schedule.getIntervalCycles());
        }
        schedules.save(schedule);
    }

    @Transactional
    public void escalateOverdue() {
        for (MaintenanceActivity a : activities.findOverdue(Instant.now())) {
            if (WorkflowEngine.COMPLETED.equals(a.getState()) || WorkflowEngine.VERIFIED.equals(a.getState())
                    || WorkflowEngine.CANCELLED.equals(a.getState()) || WorkflowEngine.ESCALATED.equals(a.getState())) {
                continue;
            }
            User system = users.findByUsername("admin").orElse(null);
            if (system == null) continue;
            try {
                applyTransition(a, WorkflowEngine.ESCALATED, system, "Automatically escalated: overdue");
                activities.save(a);
                notifyManagers("Overdue: " + a.getActivityNumber(),
                        a.getTitle() + " on " + a.getAircraft().getRegistration() + " is overdue and escalated",
                        "OVERDUE", a);
            } catch (ApiException ignored) {
                notifyManagers("Overdue: " + a.getActivityNumber(),
                        a.getTitle() + " is overdue", "OVERDUE", a);
            }
        }
    }

    public Dtos.DashboardMetrics dashboard(User actor) {
        Instant now = Instant.now();
        long overdue = activities.countOverdue(now);
        List<Dtos.ActivitySummary> dueSoon = activities.findAll().stream()
                .filter(a -> a.getDueAt() != null && a.getDueAt().isAfter(now)
                        && a.getDueAt().isBefore(now.plusSeconds(7 * 24 * 3600))
                        && !List.of("VERIFIED", "CANCELLED", "COMPLETED").contains(a.getState()))
                .map(this::summary)
                .limit(8)
                .toList();
        List<Dtos.ActivitySummary> aog = activities.findAll().stream()
                .filter(a -> "AOG".equals(a.getPriority()) && !List.of("VERIFIED", "CANCELLED").contains(a.getState()))
                .map(this::summary)
                .toList();
        return new Dtos.DashboardMetrics(
                activities.countByState(WorkflowEngine.PENDING) + activities.countByState(WorkflowEngine.ASSIGNED)
                        + activities.countByState(WorkflowEngine.IN_PROGRESS) + activities.countByState(WorkflowEngine.ON_HOLD)
                        + activities.countByState(WorkflowEngine.ESCALATED),
                activities.countByState(WorkflowEngine.IN_PROGRESS),
                overdue,
                activities.countByState(WorkflowEngine.COMPLETED),
                0,
                dueSoon,
                aog
        );
    }

    public String complianceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("AMOS Compliance Report\nGenerated: ").append(Instant.now()).append("\n\n");
        for (Object[] row : activities.countByStateGrouped()) {
            sb.append(row[0]).append(": ").append(row[1]).append("\n");
        }
        sb.append("\nOverdue: ").append(activities.countOverdue(Instant.now())).append("\n");
        sb.append("\nOpen work orders:\n");
        activities.findAll().stream()
                .filter(a -> !List.of("VERIFIED", "CANCELLED").contains(a.getState()))
                .forEach(a -> sb.append(a.getActivityNumber()).append(" | ")
                        .append(a.getAircraft().getRegistration()).append(" | ")
                        .append(a.getState()).append(" | due ").append(a.getDueAt()).append("\n"));
        return sb.toString();
    }

    private void applyTransition(MaintenanceActivity a, String to, User actor, String comment) {
        Set<String> perms = actor.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        actor.getRoles().forEach(r -> perms.add("ROLE_" + r.getCode()));
        WorkflowEngine.assertTransition(a.getState(), to, perms);
        WorkflowTransition t = new WorkflowTransition();
        t.setActivity(a);
        t.setFromState(a.getState());
        t.setToState(to);
        t.setActor(actor);
        t.setComment(comment);
        transitions.save(t);
        a.setState(to);
    }

    private MaintenanceActivity load(Long id) {
        return activities.findById(id).orElseThrow(() -> ApiException.notFound("Activity not found"));
    }

    private synchronized String nextNumber() {
        return "WO-" + Year.now().getValue() + "-" + (1000 + activities.count() + 1);
    }

    private Dtos.ActivitySummary summary(MaintenanceActivity a) {
        return new Dtos.ActivitySummary(a.getId(), a.getActivityNumber(), a.getTitle(),
                a.getAircraft().getRegistration(),
                a.getCheckType() == null ? null : a.getCheckType().getCode(),
                a.getState(), a.getPriority(), a.getDueAt(),
                a.getAssignedTo() == null ? null : a.getAssignedTo().getFullName(),
                a.getStation(), a.getParent() == null ? null : a.getParent().getId());
    }

    private Dtos.ActivityDetail detail(MaintenanceActivity a, User actor) {
        Set<String> perms = actor.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        actor.getRoles().forEach(r -> perms.add("ROLE_" + r.getCode()));
        List<Dtos.AssignmentView> assignmentViews = assignments.findByActivityId(a.getId()).stream()
                .map(x -> new Dtos.AssignmentView(x.getId(), x.getUser().getId(), x.getUser().getFullName(),
                        x.getRoleOnJob(), x.getAssignedAt()))
                .toList();
        List<Dtos.LogView> logViews = logs.findByActivityIdOrderByCreatedAtDesc(a.getId()).stream()
                .map(x -> new Dtos.LogView(x.getId(), x.getAuthor().getFullName(), x.getLogType(),
                        x.getFindings(), x.getActionsTaken(), x.getHoursSpent(), x.getCreatedAt()))
                .toList();
        List<Dtos.TransitionView> history = transitions.findByActivityIdOrderByCreatedAtAsc(a.getId()).stream()
                .map(x -> new Dtos.TransitionView(x.getId(), x.getFromState(), x.getToState(),
                        x.getActor().getFullName(), x.getComment(), x.getCreatedAt()))
                .toList();
        List<Dtos.ActivitySummary> children = activities.findByParentId(a.getId()).stream().map(this::summary).toList();
        return new Dtos.ActivityDetail(a.getId(), a.getActivityNumber(), a.getTitle(), a.getDescription(),
                a.getAircraft().getId(), a.getAircraft().getRegistration(),
                a.getCheckType() == null ? null : a.getCheckType().getId(),
                a.getCheckType() == null ? null : a.getCheckType().getCode(),
                a.getTask() == null ? null : a.getTask().getId(),
                a.getTask() == null ? null : a.getTask().getTaskCard(),
                a.getComponent() == null ? null : a.getComponent().getId(),
                a.getParent() == null ? null : a.getParent().getId(),
                a.getSchedule() == null ? null : a.getSchedule().getId(),
                a.getState(), a.getPriority(), a.getDueAt(), a.getPlannedStart(), a.getPlannedEnd(),
                a.getActualStart(), a.getActualEnd(),
                a.getAssignedTo() == null ? null : a.getAssignedTo().getId(),
                a.getAssignedTo() == null ? null : a.getAssignedTo().getFullName(),
                a.getRequestedBy() == null ? null : a.getRequestedBy().getId(),
                a.getRequestedBy() == null ? null : a.getRequestedBy().getFullName(),
                a.getVerifiedBy() == null ? null : a.getVerifiedBy().getId(),
                a.getVerifiedBy() == null ? null : a.getVerifiedBy().getFullName(),
                a.getStation(), a.getFindings(), a.getActionsTaken(),
                WorkflowEngine.allowedTargets(a.getState(), perms),
                assignmentViews, logViews, history, children);
    }

    private void notifyWatchers(MaintenanceActivity a, String title, String body, String type) {
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (a.getAssignedTo() != null) ids.add(a.getAssignedTo().getId());
        if (a.getRequestedBy() != null) ids.add(a.getRequestedBy().getId());
        if (!ids.isEmpty()) {
            notifications.emit(List.copyOf(ids), title, body, type, "MaintenanceActivity", String.valueOf(a.getId()));
        }
    }

    private void notifyManagers(String title, String body, String type, MaintenanceActivity a) {
        List<Long> ids = users.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        List.of("ADMIN", "MAINTENANCE_MANAGER", "SUPERVISOR").contains(r.getCode())))
                .map(User::getId)
                .toList();
        notifications.emit(ids, title, body, type, "MaintenanceActivity", String.valueOf(a.getId()));
    }

    private void notifyQaAndManagers(String title, String body, String type, MaintenanceActivity a) {
        List<Long> ids = users.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        List.of("ADMIN", "MAINTENANCE_MANAGER", "QUALITY_ASSURANCE", "SUPERVISOR").contains(r.getCode())))
                .map(User::getId)
                .toList();
        notifications.emit(ids, title, body, type, "MaintenanceActivity", String.valueOf(a.getId()));
    }
}
