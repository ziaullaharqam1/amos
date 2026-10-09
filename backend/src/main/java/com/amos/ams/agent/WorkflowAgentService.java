package com.amos.ams.agent;

import com.amos.ams.config.AppProperties;
import com.amos.ams.domain.User;
import com.amos.ams.dto.Dtos;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WorkflowAgentService {
    private static final String SYSTEM = """
            You are the AMOS hangar coordinator talking to a real person on shift.
            Do manager work (create, assign, reassign, escalate, hold, cancel), technician/engineer work
            (start, complete, hold), and QA work (verify completed jobs or reject them for rework).
            Flow: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → VERIFIED.
            Branches: ON_HOLD, ESCALATED, REJECTED, CANCELLED.
            Always use tools to read live data, then call create_work_order, assign_work_order, or change_status when asked.
            Reply in short spoken English with bullet lists. Never output JSON, YAML, or code fences.
            If a tool errors, say which permission or role is missing (manager, technician, or QA).
            If apply=false, still call the mutating tools (they dry-run) and explain what would change.
            If you need a person or work-order number, ask one clear question and offer options.
            """;

    private final LlmClient llm;
    private final WorkflowAgentTools tools;
    private final AppProperties properties;

    public WorkflowAgentService(LlmClient llm, WorkflowAgentTools tools, AppProperties properties) {
        this.llm = llm;
        this.tools = tools;
        this.properties = properties;
    }

    public Dtos.AgentStatus status() {
        AppProperties.Grok grok = properties.getGrok();
        return new Dtos.AgentStatus(grok.isConfigured(), grok.displayModel(), grok.getMaxSteps());
    }

    public Dtos.AgentReply run(Dtos.AgentAsk ask, User actor) {
        boolean apply = resolveApply(ask);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM));
        messages.add(Map.of("role", "user", "content",
                "Operator " + actor.getUsername() + " (apply=" + apply + "): " + ask.message()));

        List<Dtos.AgentStep> steps = new ArrayList<>();
        int max = Math.max(1, properties.getGrok().getMaxSteps());
        for (int i = 0; i < max; i++) {
            LlmClient.ChatResult result = llm.complete(messages, toolSpecs());
            if (!result.hasToolCalls()) {
                String text = result.content() == null || result.content().isBlank()
                        ? "I have no further steps. Tell me if you want to assign, start, complete, verify, or escalate work."
                        : result.content();
                if (AgentNarration.looksLikeJson(text)) {
                    text = AgentNarration.result(text);
                }
                return new Dtos.AgentReply(text, apply, steps);
            }
            messages.add(assistantMessage(result));
            for (LlmClient.ToolCall call : result.toolCalls()) {
                String output = tools.execute(call.name(), call.argumentsJson(), actor, apply);
                steps.add(new Dtos.AgentStep(
                        call.name(),
                        AgentNarration.arguments(call.name(), call.argumentsJson()),
                        AgentNarration.result(output)));
                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", call.id());
                toolMsg.put("content", clip(output));
                messages.add(toolMsg);
            }
        }
        return new Dtos.AgentReply("I paused after " + max + " steps. Send another message if you want me to continue.", apply, steps);
    }

    private static Map<String, Object> assistantMessage(LlmClient.ChatResult result) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (LlmClient.ToolCall call : result.toolCalls()) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", call.name());
            fn.put("arguments", call.argumentsJson() == null ? "{}" : call.argumentsJson());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", call.id());
            item.put("type", "function");
            item.put("function", fn);
            calls.add(item);
        }
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", result.content() == null ? "" : result.content());
        msg.put("tool_calls", calls);
        return msg;
    }

    private static String clip(String text) {
        if (text == null) return "";
        return text.length() > 8000 ? text.substring(0, 8000) + "…" : text;
    }

    static List<Map<String, Object>> toolSpecs() {
        return List.of(
                fn("list_work_orders", "List work orders. Optional filters: state, aircraftId.",
                        Map.of(
                                "state", Map.of("type", "string", "description", "Workflow state such as PENDING or ESCALATED"),
                                "aircraftId", Map.of("type", "integer")
                        )),
                fn("get_work_order", "Get one work order including allowed next states.",
                        Map.of("activityId", Map.of("type", "integer")),
                        List.of("activityId")),
                fn("list_people", "List active users, usernames, and roles for assignment.", Map.of()),
                fn("list_my_queue", "Work orders assigned to the logged-in technician or engineer.", Map.of()),
                fn("list_aircraft", "List aircraft registrations and ids for opening work.", Map.of()),
                fn("dashboard", "Open / in-progress / overdue / AOG counts and lists.", Map.of()),
                fn("create_work_order", "Open a new pending work order. Prefer registration such as A6-AMS.",
                        Map.of(
                                "title", Map.of("type", "string"),
                                "description", Map.of("type", "string"),
                                "aircraftId", Map.of("type", "integer"),
                                "registration", Map.of("type", "string"),
                                "priority", Map.of("type", "string", "description", "NORMAL, HIGH, or AOG"),
                                "station", Map.of("type", "string"),
                                "dueAt", Map.of("type", "string")
                        ),
                        List.of("title")),
                fn("assign_work_order", "Assign a work order to a technician (moves PENDING/ESCALATED to ASSIGNED).",
                        Map.of(
                                "activityId", Map.of("type", "integer"),
                                "userId", Map.of("type", "integer"),
                                "username", Map.of("type", "string"),
                                "roleOnJob", Map.of("type", "string")
                        ),
                        List.of("activityId")),
                fn("change_status", "Move a work order to a legal next state.",
                        Map.of(
                                "activityId", Map.of("type", "integer"),
                                "toState", Map.of("type", "string"),
                                "comment", Map.of("type", "string")
                        ),
                        List.of("activityId", "toState"))
        );
    }

    private static Map<String, Object> fn(String name, String description, Map<String, Object> properties) {
        return fn(name, description, properties, List.of());
    }

    private static Map<String, Object> fn(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", schema);
        return Map.of("type", "function", "function", function);
    }

    static boolean resolveApply(Dtos.AgentAsk ask) {
        String message = ask.message() == null ? "" : ask.message().toLowerCase(Locale.ROOT);
        if (message.contains("dry run") || message.contains("preview") || message.contains("don't change")
                || message.contains("do not change") || message.contains("without changing")
                || message.contains("what would you")) {
            return false;
        }
        if (message.contains("go ahead") || message.contains("make the change") || message.contains("do it now")
                || message.contains("please apply")) {
            return true;
        }
        if (ask.apply() != null) {
            return ask.apply();
        }
        return looksLikeAction(message);
    }

    static boolean looksLikeAction(String message) {
        return message.contains("assign") && !message.contains("assigned")
                || message.contains("create") || message.contains("open a work") || message.contains("new work order")
                || message.contains("open a job")
                || message.contains("escalat") || message.contains("start")
                || message.contains("complete") || message.contains("verif") || message.contains("reject")
                || message.contains("hold") || message.contains("cancel") || message.contains("rework");
    }
}
