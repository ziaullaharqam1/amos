package com.amos.ams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class Dtos {
    private Dtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record AuthResponse(String token, UserView user) {}

    public record UserView(Long id, String username, String email, String fullName, String phone, String station,
                           boolean active, Set<String> roles, Set<String> permissions) {}
    public record UserUpsert(@NotBlank String username, @NotBlank String email, String password,
                             @NotBlank String fullName, String phone, String station, boolean active,
                             List<Long> roleIds) {}

    public record RoleView(Long id, String code, String name, String description, boolean systemRole,
                           Set<String> permissions) {}
    public record RoleUpsert(@NotBlank String code, @NotBlank String name, String description, List<Long> permissionIds) {}
    public record PermissionView(Long id, String code, String name, String resource, String description) {}

    public record AircraftTypeView(Long id, String icaoCode, String manufacturer, String model, String description) {}
    public record AircraftTypeUpsert(@NotBlank String icaoCode, @NotBlank String manufacturer, @NotBlank String model, String description) {}

    public record AircraftView(Long id, String registration, Long aircraftTypeId, String aircraftType,
                               String serialNumber, String status, BigDecimal totalFlightHours, Integer totalCycles,
                               String baseStation, LocalDate inServiceDate) {}
    public record AircraftUpsert(@NotBlank String registration, @NotNull Long aircraftTypeId, @NotBlank String serialNumber,
                                 String status, BigDecimal totalFlightHours, Integer totalCycles, String baseStation,
                                 LocalDate inServiceDate) {}

    public record ComponentView(Long id, String partNumber, String serialNumber, String name, String category,
                                Long aircraftId, String aircraftRegistration, String status, BigDecimal lifeLimitHours,
                                Integer lifeLimitCycles, BigDecimal accumulatedHours, Integer accumulatedCycles) {}
    public record ComponentUpsert(@NotBlank String partNumber, @NotBlank String serialNumber, @NotBlank String name,
                                  @NotBlank String category, Long aircraftId, String status, BigDecimal lifeLimitHours,
                                  Integer lifeLimitCycles, BigDecimal accumulatedHours, Integer accumulatedCycles) {}

    public record CheckTypeView(Long id, String code, String name, String description, Integer typicalDowntimeHours) {}
    public record CheckTypeUpsert(@NotBlank String code, @NotBlank String name, String description, Integer typicalDowntimeHours) {}

    public record TaskView(Long id, String taskCard, String title, String description, Long checkTypeId, String checkType,
                           Long aircraftTypeId, String aircraftType, String ataChapter, BigDecimal estimatedHours, String skill) {}
    public record TaskUpsert(@NotBlank String taskCard, @NotBlank String title, String description, Long checkTypeId,
                             Long aircraftTypeId, String ataChapter, BigDecimal estimatedHours, String skill) {}

    public record ScheduleView(Long id, Long aircraftId, String aircraftRegistration, Long taskId, String taskCard,
                               Long checkTypeId, String checkType, BigDecimal intervalHours, Integer intervalDays,
                               Integer intervalCycles, Instant lastPerformedAt, Instant nextDueAt, BigDecimal nextDueHours,
                               Integer nextDueCycles, boolean active) {}
    public record ScheduleUpsert(@NotNull Long aircraftId, Long taskId, Long checkTypeId, BigDecimal intervalHours,
                                 Integer intervalDays, Integer intervalCycles, Instant nextDueAt, boolean active) {}

    public record ActivitySummary(Long id, String activityNumber, String title, String aircraftRegistration, String checkType,
                                  String state, String priority, Instant dueAt, String assignedTo, String station, Long parentId) {}
    public record ActivityDetail(Long id, String activityNumber, String title, String description, Long aircraftId,
                                 String aircraftRegistration, Long checkTypeId, String checkType, Long taskId, String taskCard,
                                 Long componentId, Long parentId, Long scheduleId, String state, String priority, Instant dueAt,
                                 Instant plannedStart, Instant plannedEnd, Instant actualStart, Instant actualEnd,
                                 Long assignedToId, String assignedTo, Long requestedById, String requestedBy,
                                 Long verifiedById, String verifiedBy, String station, String findings, String actionsTaken,
                                 List<String> allowedTransitions, List<AssignmentView> assignments, List<LogView> logs,
                                 List<TransitionView> history, List<ActivitySummary> children) {}
    public record ActivityCreate(@NotBlank String title, String description, @NotNull Long aircraftId, Long checkTypeId,
                                 Long taskId, Long componentId, Long parentId, Long scheduleId, String priority,
                                 Instant dueAt, Instant plannedStart, Instant plannedEnd, String station) {}
    public record ActivityUpdate(String title, String description, String priority, Instant dueAt, Instant plannedStart,
                                 Instant plannedEnd, String station, String findings, String actionsTaken) {}
    public record AssignRequest(@NotNull Long userId, String roleOnJob) {}
    public record TransitionRequest(@NotBlank String toState, String comment) {}
    public record LogRequest(String logType, String findings, String actionsTaken, BigDecimal hoursSpent) {}
    public record AssignmentView(Long id, Long userId, String fullName, String roleOnJob, Instant assignedAt) {}
    public record LogView(Long id, String author, String logType, String findings, String actionsTaken,
                          BigDecimal hoursSpent, Instant createdAt) {}
    public record TransitionView(Long id, String fromState, String toState, String actor, String comment, Instant createdAt) {}

    public record NotificationView(Long id, String title, String body, String type, String channel, String entityType,
                                   String entityId, Instant readAt, String deliveryStatus, Instant createdAt) {}
    public record AuditView(Long id, String actorUsername, String action, String entityType, String entityId,
                            String beforeValue, String afterValue, Instant createdAt) {}
    public record DashboardMetrics(long openActivities, long inProgress, long overdue, long awaitingQa,
                                   long unreadNotifications, List<ActivitySummary> dueSoon, List<ActivitySummary> aog) {}

    public record AgentAsk(@NotBlank String message, Boolean apply) {}
    public record AgentStep(String tool, String arguments, String result) {}
    public record AgentReply(String answer, boolean applied, List<AgentStep> steps) {}
    public record AgentStatus(boolean configured, String model, int maxSteps) {}
}
