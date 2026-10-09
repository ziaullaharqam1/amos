package com.amos.ams.workflow;

import com.amos.ams.exception.ApiException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorkflowEngine {
    public static final String PENDING = "PENDING";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String ON_HOLD = "ON_HOLD";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
    public static final String VERIFIED = "VERIFIED";
    public static final String CANCELLED = "CANCELLED";
    public static final String ESCALATED = "ESCALATED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING, Set.of(ASSIGNED, CANCELLED, ESCALATED),
            ASSIGNED, Set.of(IN_PROGRESS, ON_HOLD, ESCALATED, CANCELLED, PENDING),
            IN_PROGRESS, Set.of(COMPLETED, ON_HOLD, ESCALATED),
            ON_HOLD, Set.of(ASSIGNED, IN_PROGRESS, CANCELLED, ESCALATED),
            ESCALATED, Set.of(ASSIGNED, CANCELLED),
            COMPLETED, Set.of(VERIFIED, REJECTED),
            REJECTED, Set.of(IN_PROGRESS, ASSIGNED),
            VERIFIED, Set.of(),
            CANCELLED, Set.of()
    );

    private static final Map<String, String> REQUIRED_PERMISSION = Map.of(
            ASSIGNED, "ACTIVITY_ASSIGN",
            IN_PROGRESS, "ACTIVITY_WORK",
            ON_HOLD, "ACTIVITY_WORK",
            COMPLETED, "ACTIVITY_WORK",
            REJECTED, "ACTIVITY_APPROVE",
            VERIFIED, "ACTIVITY_VERIFY",
            CANCELLED, "ACTIVITY_ASSIGN",
            ESCALATED, "ACTIVITY_ESCALATE",
            PENDING, "ACTIVITY_ASSIGN"
    );

    private WorkflowEngine() {}

    public static void assertTransition(String from, String to, Set<String> permissions) {
        Set<String> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw ApiException.badRequest("Cannot move from " + from + " to " + to);
        }
        String required = REQUIRED_PERMISSION.get(to);
        if (required != null && !permissions.contains(required) && !permissions.contains("ROLE_ADMIN")) {
            throw ApiException.forbidden("Permission " + required + " is required to move to " + to);
        }
    }

    public static List<String> allowedTargets(String from, Set<String> permissions) {
        return TRANSITIONS.getOrDefault(from, Set.of()).stream()
                .filter(to -> {
                    String required = REQUIRED_PERMISSION.get(to);
                    return required == null || permissions.contains(required) || permissions.contains("ROLE_ADMIN");
                })
                .sorted()
                .toList();
    }
}
