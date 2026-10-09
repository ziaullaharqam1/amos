package com.amos.ams.agent;

import com.amos.ams.domain.Aircraft;
import com.amos.ams.domain.CheckType;
import com.amos.ams.domain.User;
import com.amos.ams.dto.Dtos;
import com.amos.ams.repository.AircraftRepository;
import com.amos.ams.repository.CheckTypeRepository;
import com.amos.ams.repository.UserRepository;
import com.amos.ams.service.ActivityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowAgentTools {
    private final ActivityService activities;
    private final UserRepository users;
    private final AircraftRepository aircraft;
    private final CheckTypeRepository checkTypes;
    private final ObjectMapper mapper;

    public WorkflowAgentTools(ActivityService activities, UserRepository users, AircraftRepository aircraft,
                              CheckTypeRepository checkTypes, ObjectMapper mapper) {
        this.activities = activities;
        this.users = users;
        this.aircraft = aircraft;
        this.checkTypes = checkTypes;
        this.mapper = mapper;
    }

    public String execute(String name, String argumentsJson, User actor, boolean apply) {
        try {
            JsonNode args = argumentsJson == null || argumentsJson.isBlank()
                    ? mapper.readTree("{}")
                    : mapper.readTree(argumentsJson);
            return switch (name) {
                case "list_work_orders" -> listWork(args);
                case "get_work_order" -> getWork(args, actor);
                case "list_people" -> listPeople();
                case "list_aircraft" -> listAircraft();
                case "list_my_queue" -> mapper.writeValueAsString(activities.workQueue(actor));
                case "dashboard" -> mapper.writeValueAsString(activities.dashboard(actor));
                case "create_work_order" -> create(args, actor, apply);
                case "assign_work_order" -> assign(args, actor, apply);
                case "change_status" -> changeStatus(args, actor, apply);
                default -> mapper.writeValueAsString(Map.of("error", "Unknown tool: " + name));
            };
        } catch (Exception e) {
            try {
                return mapper.writeValueAsString(Map.of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            } catch (Exception json) {
                return "{\"error\":\"tool failed\"}";
            }
        }
    }

    private String listWork(JsonNode args) throws Exception {
        String state = text(args, "state");
        Long aircraftId = args.hasNonNull("aircraftId") ? args.get("aircraftId").asLong() : null;
        return mapper.writeValueAsString(activities.list(state, aircraftId));
    }

    private String getWork(JsonNode args, User actor) throws Exception {
        long id = args.path("activityId").asLong(0);
        if (id == 0) {
            return mapper.writeValueAsString(Map.of("error", "activityId is required"));
        }
        return mapper.writeValueAsString(activities.get(id, actor));
    }

    private String listPeople() throws Exception {
        List<Map<String, Object>> people = users.findByActiveTrue().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "fullName", u.getFullName(),
                        "station", u.getStation() == null ? "" : u.getStation(),
                        "roles", u.getRoles().stream().map(r -> r.getCode()).toList()
                ))
                .toList();
        return mapper.writeValueAsString(people);
    }

    private String listAircraft() throws Exception {
        List<Map<String, Object>> fleet = aircraft.findAll().stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "registration", a.getRegistration(),
                        "status", a.getStatus() == null ? "" : a.getStatus(),
                        "station", a.getBaseStation() == null ? "" : a.getBaseStation()
                ))
                .toList();
        return mapper.writeValueAsString(fleet);
    }

    private String create(JsonNode args, User actor, boolean apply) throws Exception {
        String title = text(args, "title");
        Long aircraftId = resolveAircraftId(args);
        if (title == null || aircraftId == null) {
            return mapper.writeValueAsString(Map.of("error", "title and aircraft registration (or aircraftId) are required"));
        }
        String priority = text(args, "priority");
        if (priority == null) priority = "NORMAL";
        String description = text(args, "description");
        if (description == null) description = title;
        String station = text(args, "station");
        if (station == null) station = actor.getStation();
        Instant dueAt = parseInstant(text(args, "dueAt"));
        if (dueAt == null) {
            dueAt = "AOG".equalsIgnoreCase(priority)
                    ? Instant.now().plus(6, ChronoUnit.HOURS)
                    : Instant.now().plus(2, ChronoUnit.DAYS);
        }
        Long checkTypeId = checkTypes.findByCode("UNSCHE").map(CheckType::getId).orElse(null);
        String registration = aircraft.findById(aircraftId).map(Aircraft::getRegistration).orElse("aircraft " + aircraftId);
        if (!apply) {
            return mapper.writeValueAsString(Map.of(
                    "dryRun", true,
                    "would", "open a pending work order on " + registration + ": " + title
            ));
        }
        Dtos.ActivityDetail detail = activities.create(new Dtos.ActivityCreate(
                title, description, aircraftId, checkTypeId, null, null, null, null,
                priority, dueAt, null, null, station), actor);
        return mapper.writeValueAsString(Map.of(
                "ok", true,
                "created", true,
                "activityNumber", detail.activityNumber(),
                "state", detail.state(),
                "aircraftRegistration", detail.aircraftRegistration() == null ? "" : detail.aircraftRegistration()
        ));
    }

    private String assign(JsonNode args, User actor, boolean apply) throws Exception {
        long activityId = args.path("activityId").asLong(0);
        Long userId = resolveUserId(args);
        String roleOnJob = text(args, "roleOnJob");
        if (activityId == 0 || userId == null) {
            return mapper.writeValueAsString(Map.of("error", "activityId and userId or username are required"));
        }
        if (!apply) {
            return mapper.writeValueAsString(Map.of(
                    "dryRun", true,
                    "would", "assign activity " + activityId + " to user " + userId
            ));
        }
        Dtos.ActivityDetail detail = activities.assign(activityId, new Dtos.AssignRequest(userId, roleOnJob), actor);
        return mapper.writeValueAsString(Map.of(
                "ok", true,
                "activityNumber", detail.activityNumber(),
                "state", detail.state(),
                "assignedTo", detail.assignedTo()
        ));
    }

    private String changeStatus(JsonNode args, User actor, boolean apply) throws Exception {
        long activityId = args.path("activityId").asLong(0);
        String toState = text(args, "toState");
        String comment = text(args, "comment");
        if (activityId == 0 || toState == null) {
            return mapper.writeValueAsString(Map.of("error", "activityId and toState are required"));
        }
        if (!apply) {
            return mapper.writeValueAsString(Map.of(
                    "dryRun", true,
                    "would", "move activity " + activityId + " to " + toState
            ));
        }
        Dtos.ActivityDetail detail = activities.transition(activityId,
                new Dtos.TransitionRequest(toState, comment == null ? "Agent workflow step" : comment), actor);
        return mapper.writeValueAsString(Map.of(
                "ok", true,
                "activityNumber", detail.activityNumber(),
                "state", detail.state()
        ));
    }

    private Long resolveAircraftId(JsonNode args) {
        if (args.hasNonNull("aircraftId")) {
            return args.get("aircraftId").asLong();
        }
        String registration = text(args, "registration");
        if (registration == null) return null;
        return aircraft.findByRegistration(registration.toUpperCase(java.util.Locale.ROOT))
                .map(Aircraft::getId).orElse(null);
    }

    private Long resolveUserId(JsonNode args) {
        if (args.hasNonNull("userId")) {
            return args.get("userId").asLong();
        }
        String username = text(args, "username");
        if (username == null) return null;
        return users.findByUsername(username).map(User::getId).orElse(null);
    }

    private static Instant parseInstant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode args, String field) {
        if (!args.hasNonNull(field)) return null;
        String value = args.get(field).asText();
        return value == null || value.isBlank() ? null : value;
    }
}
