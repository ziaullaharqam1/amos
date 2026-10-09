package com.amos.ams.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns tool JSON into spoken hangar language (never a raw dump). */
final class AgentNarration {
    private AgentNarration() {}

    static String arguments(String tool, String argumentsJson) {
        JsonNode args = parse(argumentsJson);
        if (args == null || !args.isObject() || args.isEmpty()) {
            return switch (tool) {
                case "dashboard" -> "Checked the hangar board";
                case "list_work_orders" -> "Listed work orders";
                case "list_people" -> "Listed people who can take work";
                case "list_aircraft" -> "Listed the fleet";
                case "list_my_queue" -> "Opened your personal queue";
                default -> prettyTool(tool);
            };
        }
        String id = args.hasNonNull("activityId") ? " work order #" + args.get("activityId").asText() : "";
        String user = args.hasNonNull("username") ? args.get("username").asText()
                : args.hasNonNull("userId") ? "user #" + args.get("userId").asText() : "";
        String state = args.hasNonNull("toState") ? args.get("toState").asText().replace('_', ' ') : "";
        String filter = args.hasNonNull("state") ? " in " + args.get("state").asText().replace('_', ' ') : "";
        return switch (tool) {
            case "create_work_order" -> {
                String title = args.path("title").asText("");
                String ac = args.hasNonNull("registration") ? args.get("registration").asText()
                        : args.hasNonNull("aircraftId") ? "aircraft #" + args.get("aircraftId").asText() : "";
                yield "Open work order" + (title.isBlank() ? "" : " “" + title + "”") + (ac.isBlank() ? "" : " on " + ac);
            }
            case "assign_work_order" -> "Assign" + id + (user.isBlank() ? "" : " to " + user);
            case "change_status" -> "Move" + id + (state.isBlank() ? "" : " to " + state);
            case "get_work_order" -> "Look up" + id;
            case "list_work_orders" -> "List work orders" + filter;
            default -> prettyTool(tool);
        };
    }

    static String result(String json) {
        JsonNode root = parse(json);
        if (root == null) {
            return json == null || json.isBlank() ? "No details." : json;
        }
        if (root.hasNonNull("error")) {
            return "That step did not go through: " + root.get("error").asText();
        }
        if (root.path("dryRun").asBoolean(false)) {
            return "Preview only — " + root.path("would").asText("no database change yet") + ".";
        }
        if (root.path("ok").asBoolean(false)) {
            String wo = root.path("activityNumber").asText("the work order");
            String state = prettyState(root.path("state").asText(""));
            String who = root.path("assignedTo").asText("");
            String ac = root.path("aircraftRegistration").asText("");
            if (root.path("created").asBoolean(false)) {
                return "Opened " + wo + (ac.isBlank() ? "" : " on " + ac)
                        + (state.isBlank() ? "." : ". Status is " + state + ".");
            }
            if (!who.isBlank()) {
                return "Done: " + wo + " is assigned to " + who + (state.isBlank() ? "." : " and is now " + state + ".");
            }
            return "Done: " + wo + (state.isBlank() ? " was updated." : " is now " + state + ".");
        }
        if (root.isArray()) {
            return formatWorkOrPeople(root);
        }
        if (root.has("openActivities") || root.has("overdue") || root.has("aog")) {
            return formatDashboard(root);
        }
        if (root.has("activityNumber")) {
            return "• " + lineForWork(root);
        }
        return "I have the live data and will use it in the answer above.";
    }

    static String fromToolMessages(List<java.util.Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map<String, Object> msg : messages) {
            if (!"tool".equals(String.valueOf(msg.get("role")))) continue;
            String content = String.valueOf(msg.getOrDefault("content", ""));
            String told = result(content);
            if (told.contains("I have the live data")) continue;
            if (sb.indexOf(told) >= 0) continue;
            sb.append(told).append('\n');
        }
        return sb.toString().trim();
    }

    static boolean looksLikeJson(String text) {
        if (text == null) return false;
        String t = text.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    private static String formatDashboard(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hangar snapshot:\n");
        sb.append("• ").append(root.path("openActivities").asLong(0)).append(" open, ");
        sb.append(root.path("inProgress").asLong(0)).append(" in progress, ");
        sb.append(root.path("overdue").asLong(0)).append(" overdue, ");
        sb.append(root.path("awaitingQa").asLong(0)).append(" waiting for QA.\n");
        appendNamedList(sb, "AOG", root.get("aog"));
        appendNamedList(sb, "Due soon", root.get("dueSoon"));
        return sb.toString().trim();
    }

    private static void appendNamedList(StringBuilder sb, String title, JsonNode list) {
        if (list == null || !list.isArray() || list.isEmpty()) return;
        sb.append(title).append(":\n");
        for (JsonNode row : list) {
            if (row.has("activityNumber")) sb.append("• ").append(lineForWork(row)).append('\n');
        }
    }

    private static String formatWorkOrPeople(JsonNode array) {
        boolean work = false;
        boolean people = false;
        List<String> lines = new ArrayList<>();
        for (JsonNode row : array) {
            if (row.has("activityNumber")) {
                work = true;
                lines.add("• " + lineForWork(row));
            } else if (row.has("username")) {
                people = true;
                String roles = row.path("roles").isArray() ? row.get("roles").toString()
                        .replaceAll("[\\[\\]\"]", "") : "";
                lines.add("• " + row.path("fullName").asText(row.path("username").asText())
                        + " (" + row.path("username").asText() + ")"
                        + (roles.isBlank() ? "" : " — " + roles.replace("MAINTENANCE_", "").replace('_', ' ')));
            } else if (row.has("registration")) {
                lines.add("• " + row.path("registration").asText()
                        + (row.path("status").asText("").isBlank() ? "" : " (" + row.path("status").asText() + ")"));
            }
        }
        if (lines.isEmpty()) {
            return work || people ? "Nothing in that list." : "Empty result.";
        }
        String header = work ? "Work orders:\n" : people ? "People:\n" : "Fleet:\n";
        return header + String.join("\n", lines);
    }

    static String lineForWork(JsonNode row) {
        String due = row.path("dueAt").asText("");
        if (due.length() > 16) due = due.substring(0, 16).replace('T', ' ');
        String assigned = row.path("assignedTo").asText("");
        return row.path("activityNumber").asText("WO")
                + " — " + row.path("title").asText("untitled")
                + " (" + prettyState(row.path("state").asText("?"))
                + (row.path("priority").asText("").isBlank() ? "" : ", " + row.path("priority").asText())
                + (assigned.isBlank() ? "" : ", assigned to " + assigned)
                + (due.isBlank() ? "" : ", due " + due)
                + ")";
    }

    static String prettyState(String state) {
        if (state == null || state.isBlank()) return "";
        return state.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    static String prettyTool(String tool) {
        return tool == null ? "step" : tool.replace('_', ' ');
    }

    private static JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
