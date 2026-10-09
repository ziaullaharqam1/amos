package com.amos.ams.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in agent that uses the same workflow tools as Grok, with no cloud credits.
 * Handles manager (assign / escalate), technician-engineer (start / complete / hold),
 * and QA (verify / reject) in plain language.
 */
public class LocalFreeLlmClient {
    private static final Pattern WO_NUMBER = Pattern.compile("\\bWO-\\d+(?:-\\d+)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAIL = Pattern.compile("\\bA6-[A-Z0-9]+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern KNOWN_USER = Pattern.compile(
            "\\b(tech1|tech2|manager|supervisor|admin|qa|engineer)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> CLOSED = List.of("VERIFIED", "CANCELLED");
    private static final List<String> ESCALATABLE = List.of("PENDING", "ASSIGNED", "IN_PROGRESS", "ON_HOLD");

    enum Intent {
        CREATE, ASSIGN, START, COMPLETE, VERIFY, HOLD, ESCALATE, REJECT, CANCEL, BRIEF
    }

    private final ObjectMapper mapper;

    public LocalFreeLlmClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public LlmClient.ChatResult complete(List<Map<String, Object>> messages) {
        String packed = lastUser(messages);
        String request = requestOnly(packed).toLowerCase(Locale.ROOT);
        Intent intent = detect(request);
        Set<String> used = toolsUsed(messages);
        int seq = used.size() + 1;

        boolean needPeople = intent == Intent.ASSIGN && !used.contains("list_people");
        boolean needMine = (intent == Intent.START || intent == Intent.COMPLETE || intent == Intent.HOLD)
                && !used.contains("list_my_queue");
        boolean needFleet = intent == Intent.CREATE && !used.contains("list_aircraft");

        if (!used.contains("dashboard") || !used.contains("list_work_orders") || needPeople || needMine || needFleet) {
            List<LlmClient.ToolCall> calls = new ArrayList<>();
            if (!used.contains("dashboard")) {
                calls.add(call(seq++, "dashboard", "{}"));
            }
            if (!used.contains("list_work_orders")) {
                calls.add(call(seq++, "list_work_orders", listArgs(intent, request)));
            }
            if (needMine) {
                calls.add(call(seq++, "list_my_queue", "{}"));
            }
            if (needPeople) {
                calls.add(call(seq++, "list_people", "{}"));
            }
            if (needFleet) {
                calls.add(call(seq++, "list_aircraft", "{}"));
            }
            return new LlmClient.ChatResult(null, calls);
        }

        List<JsonNode> work = workOrders(messages);
        String woFilter = mentionedWorkOrder(request);

        if (intent == Intent.CREATE && !used.contains("create_work_order")) {
            String registration = resolveRegistration(request, messages);
            String title = extractTitle(request);
            if (registration == null) {
                return new LlmClient.ChatResult(askWhichAircraft(messages), List.of());
            }
            String priority = request.contains("aog") ? "AOG" : request.contains("high") ? "HIGH" : "NORMAL";
            try {
                java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("title", title);
                payload.put("description", title);
                payload.put("registration", registration);
                payload.put("priority", priority);
                return new LlmClient.ChatResult(null, List.of(
                        call(seq, "create_work_order", mapper.writeValueAsString(payload))));
            } catch (Exception e) {
                return new LlmClient.ChatResult("I could not build the create request. Name the aircraft and defect again.", List.of());
            }
        }

        if (intent == Intent.ASSIGN && !used.contains("assign_work_order")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("PENDING", "ESCALATED"));
            String username = resolveUsername(request, messages);
            if (targets.isEmpty()) {
                return new LlmClient.ChatResult(assignHelp(work, woFilter), List.of());
            }
            if (username == null) {
                return new LlmClient.ChatResult(askWho(targets, messages), List.of());
            }
            List<LlmClient.ToolCall> calls = new ArrayList<>();
            for (JsonNode row : cap(targets)) {
                calls.add(call(seq++, "assign_work_order",
                        "{\"activityId\":" + row.get("id").asLong()
                                + ",\"username\":\"" + username + "\",\"roleOnJob\":\"TECHNICIAN\"}"));
            }
            return new LlmClient.ChatResult(null, calls);
        }

        if (intent == Intent.START && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("ASSIGNED", "ON_HOLD", "REJECTED"));
            return statusCalls(seq, targets, "IN_PROGRESS", "Agent: start work",
                    emptyStatus("I do not see assigned work to start.", work, woFilter));
        }

        if (intent == Intent.COMPLETE && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("IN_PROGRESS"));
            return statusCalls(seq, targets, "COMPLETED", "Agent: work finished, ready for QA",
                    emptyStatus("Nothing is in progress to complete.", work, woFilter));
        }

        if (intent == Intent.VERIFY && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("COMPLETED"));
            return statusCalls(seq, targets, "VERIFIED", "Agent: QA release",
                    emptyStatus("Nothing is waiting for QA verification.", work, woFilter));
        }

        if (intent == Intent.REJECT && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("COMPLETED"));
            return statusCalls(seq, targets, "REJECTED", "Agent: returned for rework",
                    emptyStatus("Nothing completed is waiting that I can return.", work, woFilter));
        }

        if (intent == Intent.HOLD && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("ASSIGNED", "IN_PROGRESS"));
            return statusCalls(seq, targets, "ON_HOLD", "Agent: held",
                    emptyStatus("I do not see active work to hold.", work, woFilter));
        }

        if (intent == Intent.ESCALATE && !used.contains("change_status")) {
            List<JsonNode> targets = escalateTargets(work, request, woFilter);
            return statusCalls(seq, targets, "ESCALATED", "Agent: overdue / blocked",
                    emptyStatus("Nothing looks overdue or AOG that I can escalate.", work, woFilter));
        }

        if (intent == Intent.CANCEL && !used.contains("change_status")) {
            List<JsonNode> targets = filterWork(work, woFilter, List.of("PENDING", "ASSIGNED", "ON_HOLD", "ESCALATED"));
            if (woFilter == null) {
                return new LlmClient.ChatResult(
                        "Cancelling is destructive. Name the work order (for example “cancel WO-1002”).", List.of());
            }
            return statusCalls(seq, targets, "CANCELLED", "Agent: cancelled",
                    emptyStatus("I could not find that work order to cancel.", work, woFilter));
        }

        return new LlmClient.ChatResult(summarize(messages, packed, request, intent, work), List.of());
    }

    private LlmClient.ChatResult statusCalls(int seq, List<JsonNode> targets, String toState, String comment,
                                             String emptyMessage) {
        if (targets.isEmpty()) {
            return new LlmClient.ChatResult(emptyMessage, List.of());
        }
        List<LlmClient.ToolCall> calls = new ArrayList<>();
        int n = seq;
        for (JsonNode row : cap(targets)) {
            calls.add(call(n++, "change_status",
                    "{\"activityId\":" + row.get("id").asLong()
                            + ",\"toState\":\"" + toState + "\",\"comment\":\"" + comment + "\"}"));
        }
        return new LlmClient.ChatResult(null, calls);
    }

    private static LlmClient.ToolCall call(int seq, String name, String args) {
        return new LlmClient.ToolCall("local_" + seq, name, args);
    }

    static Intent detect(String request) {
        if (request.contains("create") || request.contains("open a work") || request.contains("new work order")
                || request.contains("raise a") || request.contains("file a defect") || request.contains("open a job")
                || request.contains("open wo")) {
            return Intent.CREATE;
        }
        if (request.contains("escalat")) return Intent.ESCALATE;
        if (request.contains("verif") || request.contains("sign off") || request.contains("sign-off")
                || request.contains("qa release") || request.contains("release to service")) {
            return Intent.VERIFY;
        }
        if (request.contains("reject") || request.contains("rework") || request.contains("send back")) {
            return Intent.REJECT;
        }
        if (request.contains("cancel")) return Intent.CANCEL;
        if (request.contains("hold") || request.contains("pause")) return Intent.HOLD;
        if (request.contains("complete") || request.contains("finish") || request.contains("mark done")) {
            return Intent.COMPLETE;
        }
        if (request.contains("start") || request.contains("begin") || request.contains("in progress")
                || request.contains("pick up") || request.contains("clock in")) {
            return Intent.START;
        }
        if (request.matches(".*\\b(re)?assign\\b.*") || request.contains("hand to") || request.contains("dispatch")
                || request.contains("give to")) {
            return Intent.ASSIGN;
        }
        return Intent.BRIEF;
    }

    private static String listArgs(Intent intent, String request) {
        if (mentionedWorkOrder(request) != null) return "{}";
        return switch (intent) {
            case ASSIGN -> "{\"state\":\"PENDING\"}";
            case COMPLETE -> "{\"state\":\"IN_PROGRESS\"}";
            case VERIFY, REJECT -> "{\"state\":\"COMPLETED\"}";
            default -> "{}";
        };
    }

    private static String lastUser(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(String.valueOf(messages.get(i).get("role")))) {
                return String.valueOf(messages.get(i).getOrDefault("content", ""));
            }
        }
        return "";
    }

    /** Strip "Operator manager (apply=true):" so we do not treat the login as the assignee. */
    static String requestOnly(String packed) {
        int idx = packed.indexOf("): ");
        if (idx >= 0 && idx + 3 < packed.length()) {
            return packed.substring(idx + 3);
        }
        return packed;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> toolsUsed(List<Map<String, Object>> messages) {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> msg : messages) {
            Object raw = msg.get("tool_calls");
            if (!(raw instanceof List<?> list)) continue;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> call)) continue;
                Object fn = call.get("function");
                if (fn instanceof Map<?, ?> function) {
                    Object name = function.get("name");
                    if (name != null) names.add(String.valueOf(name));
                }
            }
        }
        return names;
    }

    private String resolveUsername(String request, List<Map<String, Object>> messages) {
        Matcher known = KNOWN_USER.matcher(request);
        if (known.find()) {
            return known.group(1).toLowerCase(Locale.ROOT);
        }
        JsonNode people = peopleArray(messages);
        if (people != null) {
            for (JsonNode person : people) {
                String username = person.path("username").asText("");
                String full = person.path("fullName").asText("");
                if (!username.isBlank() && request.contains(username.toLowerCase(Locale.ROOT))) {
                    return username;
                }
                if (!full.isBlank() && request.contains(full.toLowerCase(Locale.ROOT))) {
                    return username;
                }
            }
            if (request.contains("tech") || request.contains("technician")) {
                for (JsonNode person : people) {
                    String roles = person.path("roles").toString();
                    if (roles.contains("MAINTENANCE_TECHNICIAN")) {
                        return person.path("username").asText(null);
                    }
                }
            }
        }
        return null;
    }

    private JsonNode peopleArray(List<Map<String, Object>> messages) {
        for (Map<String, Object> msg : messages) {
            if (!"tool".equals(String.valueOf(msg.get("role")))) continue;
            String content = String.valueOf(msg.getOrDefault("content", ""));
            try {
                JsonNode root = mapper.readTree(content);
                if (root.isArray() && root.size() > 0 && root.get(0).has("username") && !root.get(0).has("activityNumber")) {
                    return root;
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return null;
    }

    private List<JsonNode> workOrders(List<Map<String, Object>> messages) {
        List<JsonNode> ids = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Map<String, Object> msg : messages) {
            if (!"tool".equals(String.valueOf(msg.get("role")))) continue;
            String content = String.valueOf(msg.getOrDefault("content", ""));
            try {
                JsonNode root = mapper.readTree(content);
                collectWork(root, ids, seen);
            } catch (Exception ignored) {
                // not work-order JSON
            }
        }
        return ids;
    }

    private void collectWork(JsonNode root, List<JsonNode> out, Set<Long> seen) {
        if (root == null) return;
        if (root.isArray()) {
            for (JsonNode row : root) collectWork(row, out, seen);
            return;
        }
        if (root.has("activityNumber") && root.has("id")) {
            long id = root.get("id").asLong();
            if (seen.add(id)) out.add(root);
        }
    }

    private static String mentionedWorkOrder(String request) {
        Matcher m = WO_NUMBER.matcher(request);
        return m.find() ? m.group().toUpperCase(Locale.ROOT) : null;
    }

    private String resolveRegistration(String request, List<Map<String, Object>> messages) {
        Matcher m = TAIL.matcher(request);
        if (m.find()) {
            return m.group().toUpperCase(Locale.ROOT);
        }
        JsonNode fleet = fleetArray(messages);
        if (fleet == null) return null;
        for (JsonNode row : fleet) {
            String reg = row.path("registration").asText("");
            if (!reg.isBlank() && request.contains(reg.toLowerCase(Locale.ROOT))) {
                return reg.toUpperCase(Locale.ROOT);
            }
        }
        if (fleet.size() == 1) {
            return fleet.get(0).path("registration").asText(null);
        }
        return null;
    }

    private JsonNode fleetArray(List<Map<String, Object>> messages) {
        for (Map<String, Object> msg : messages) {
            if (!"tool".equals(String.valueOf(msg.get("role")))) continue;
            String content = String.valueOf(msg.getOrDefault("content", ""));
            try {
                JsonNode root = mapper.readTree(content);
                if (root.isArray() && root.size() > 0 && root.get(0).has("registration")
                        && !root.get(0).has("activityNumber")) {
                    return root;
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return null;
    }

    static String extractTitle(String request) {
        String title = request;
        title = title.replaceAll("(?i)(please\\s+)?(create|open|raise|file|log)\\s+(a\\s+)?(new\\s+)?(work\\s*order|job|defect|wo)\\s*", " ");
        title = title.replaceAll("(?i)\\b(on|for|against)\\s+a6-[a-z0-9]+\\b", " ");
        title = title.replaceAll("(?i)\\ba6-[a-z0-9]+\\b", " ");
        title = title.replaceAll("(?i)\\b(aog|high priority|priority high|unscheduled)\\b", " ");
        title = title.replaceAll("(?i)^(for|on)\\s+", "");
        title = title.replaceAll("\\s+", " ").trim();
        if (title.isBlank() || title.length() < 3) {
            return "Unscheduled defect";
        }
        return title.substring(0, 1).toUpperCase(Locale.ROOT) + title.substring(1);
    }

    private String askWhichAircraft(List<Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder("I can open a work order, but I need the tail number.\n");
        JsonNode fleet = fleetArray(messages);
        if (fleet != null) {
            sb.append(AgentNarration.result(fleet.toString()));
            sb.append("\nFor example: “create a work order on A6-DREAM for pack 2 inop”.");
        } else {
            sb.append("For example: “create a work order on A6-AMS for hydraulic leak”.");
        }
        return sb.toString();
    }

    private static List<JsonNode> filterWork(List<JsonNode> work, String woNumber, List<String> states) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode row : work) {
            String state = row.path("state").asText("");
            if (states != null && !states.contains(state)) continue;
            if (woNumber != null && !woNumber.equalsIgnoreCase(row.path("activityNumber").asText())) continue;
            if (CLOSED.contains(state)) continue;
            out.add(row);
        }
        return out;
    }

    private static List<JsonNode> escalateTargets(List<JsonNode> work, String request, String woNumber) {
        Instant now = Instant.now();
        List<JsonNode> out = new ArrayList<>();
        boolean wantOverdue = request.contains("overdue") || request.contains("late");
        boolean wantAog = request.contains("aog");
        for (JsonNode row : work) {
            String state = row.path("state").asText("");
            if (!ESCALATABLE.contains(state)) continue;
            if (woNumber != null && !woNumber.equalsIgnoreCase(row.path("activityNumber").asText())) continue;
            boolean aog = "AOG".equalsIgnoreCase(row.path("priority").asText());
            boolean overdue = isOverdue(row.path("dueAt").asText(""), now);
            if (wantOverdue && !(overdue || aog)) continue;
            if (wantAog && !aog) continue;
            if (!wantOverdue && !wantAog && woNumber == null && !(overdue || aog)) continue;
            out.add(row);
        }
        return out;
    }

    private static boolean isOverdue(String dueAt, Instant now) {
        if (dueAt == null || dueAt.isBlank() || "null".equals(dueAt)) return false;
        try {
            return Instant.parse(dueAt).isBefore(now);
        } catch (Exception e) {
            try {
                return Instant.parse(dueAt + "Z").isBefore(now);
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private static List<JsonNode> cap(List<JsonNode> rows) {
        return rows.size() <= 8 ? rows : rows.subList(0, 8);
    }

    private String askWho(List<JsonNode> targets, List<Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("I can assign ");
        sb.append(targets.size() == 1 ? "this work order" : "these work orders");
        sb.append(", but I need a person:\n");
        for (JsonNode row : cap(targets)) {
            sb.append("• ").append(AgentNarration.lineForWork(row)).append('\n');
        }
        sb.append("Who should take it? For example: “assign to tech1”.");
        JsonNode people = peopleArray(messages);
        if (people != null) {
            sb.append("\nPeople on shift:\n");
            sb.append(AgentNarration.result(people.toString()));
        }
        return sb.toString();
    }

    private static String assignHelp(List<JsonNode> work, String woFilter) {
        if (woFilter != null) {
            return "I could not find " + woFilter + " in a pending or escalated state. "
                    + "Open work I can see:\n" + briefList(work);
        }
        return "There is no pending work to assign right now.\n" + briefList(work)
                + "\nSay something like “assign WO-1002 to tech1” if you want a specific job.";
    }

    private static String emptyStatus(String lead, List<JsonNode> work, String woFilter) {
        StringBuilder sb = new StringBuilder(lead);
        if (woFilter != null) sb.append(" I looked for ").append(woFilter).append('.');
        sb.append('\n').append(briefList(work));
        sb.append("\nTell me the next step in plain language: assign, start, complete, verify, or escalate.");
        return sb.toString();
    }

    private static String briefList(List<JsonNode> work) {
        if (work.isEmpty()) return "No open work orders came back from the hangar board.";
        StringBuilder sb = new StringBuilder("Current work:\n");
        for (JsonNode row : cap(work)) {
            sb.append("• ").append(AgentNarration.lineForWork(row)).append('\n');
        }
        return sb.toString().trim();
    }

    private String summarize(List<Map<String, Object>> messages, String packed, String request, Intent intent,
                             List<JsonNode> work) {
        boolean dry = packed.toLowerCase(Locale.ROOT).contains("apply=false");
        StringBuilder sb = new StringBuilder();
        sb.append(greeting(intent));
        if (dry && intent != Intent.BRIEF) {
            sb.append("This was a preview — I did not change the hangar board. ")
                    .append("Ask again with apply on, or say “go ahead”, to make it stick.\n\n");
        }

        List<String> mutations = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            if (!"tool".equals(String.valueOf(msg.get("role")))) continue;
            String content = String.valueOf(msg.getOrDefault("content", ""));
            JsonNode root;
            try {
                root = mapper.readTree(content);
            } catch (Exception e) {
                continue;
            }
            if (root == null || root.isArray()) continue;
            if (root.path("ok").asBoolean(false) || root.path("dryRun").asBoolean(false) || root.has("error")) {
                mutations.add(AgentNarration.result(content));
            }
        }
        if (!mutations.isEmpty()) {
            sb.append(String.join("\n", mutations)).append("\n\n");
        }

        String board = AgentNarration.fromToolMessages(messages);
        if (!board.isBlank() && mutations.isEmpty()) {
            sb.append(board).append("\n");
        } else if (mutations.isEmpty()) {
            sb.append(briefList(work)).append('\n');
        }

        sb.append('\n').append(nextSteps(intent, request, work, dry));
        String text = sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
        if (AgentNarration.looksLikeJson(text)) {
            return briefList(work) + "\n" + nextSteps(intent, request, work, dry);
        }
        return text;
    }

    private static String greeting(Intent intent) {
        return switch (intent) {
            case CREATE -> "Opened a work order:\n";
            case ASSIGN -> "Manager action — assignment:\n";
            case ESCALATE -> "Manager action — escalation:\n";
            case START, COMPLETE, HOLD -> "Technician / engineer action:\n";
            case VERIFY, REJECT -> "QA action:\n";
            case CANCEL -> "Planning action:\n";
            case BRIEF -> "Here is where the hangar stands.\n";
        };
    }

    private static String nextSteps(Intent intent, String request, List<JsonNode> work, boolean dry) {
        StringBuilder sb = new StringBuilder("You can say, for example:\n");
        sb.append("• Open a work order on A6-DREAM for pack 2 inop\n");
        sb.append("• Assign pending jobs to tech1\n");
        sb.append("• Start my assigned work\n");
        sb.append("• Complete in-progress jobs\n");
        sb.append("• Verify work waiting for QA\n");
        sb.append("• Escalate overdue or AOG work\n");
        if (dry) sb.append("• Go ahead (apply the last plan)\n");
        if (intent == Intent.BRIEF && request.contains("help")) {
            sb.append("I can run manager, engineer, and QA steps in one place — just tell me the outcome you want.");
        }
        return sb.toString();
    }
}
