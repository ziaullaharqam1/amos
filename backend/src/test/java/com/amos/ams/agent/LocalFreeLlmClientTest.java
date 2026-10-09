package com.amos.ams.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalFreeLlmClientTest {

    private final LocalFreeLlmClient client = new LocalFreeLlmClient(new ObjectMapper());

    @Test
    void firstTurnReadsDashboardAndWork() {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", "Operator manager (apply=false): What work is overdue or AOG?"));

        LlmClient.ChatResult result = client.complete(messages);

        assertTrue(result.hasToolCalls());
        assertEquals("dashboard", result.toolCalls().get(0).name());
        assertEquals("list_work_orders", result.toolCalls().get(1).name());
    }

    @Test
    void assignUsesTechFromRequestNotLoggedInManager() {
        LlmClient.ChatResult result = client.complete(loadedBoard(
                "Operator manager (apply=true): Assign pending jobs to tech1",
                "[{\"id\":4,\"activityNumber\":\"WO-1002\",\"state\":\"PENDING\",\"title\":\"AOG pack\",\"priority\":\"AOG\"}]",
                true));

        assertTrue(result.hasToolCalls());
        assertEquals("assign_work_order", result.toolCalls().get(0).name());
        assertTrue(result.toolCalls().get(0).argumentsJson().contains("\"username\":\"tech1\""));
        assertFalse(result.toolCalls().get(0).argumentsJson().contains("manager"));
    }

    @Test
    void verifyMovesCompletedWork() {
        LlmClient.ChatResult result = client.complete(loadedBoard(
                "Operator qa (apply=true): Verify work waiting for QA",
                "[{\"id\":8,\"activityNumber\":\"WO-0998\",\"state\":\"COMPLETED\",\"title\":\"Boroscope\"}]",
                false));

        assertTrue(result.hasToolCalls());
        assertEquals("change_status", result.toolCalls().get(0).name());
        assertTrue(result.toolCalls().get(0).argumentsJson().contains("VERIFIED"));
        assertTrue(result.toolCalls().get(0).argumentsJson().contains("\"activityId\":8"));
    }

    @Test
    void startMovesAssignedWork() {
        LlmClient.ChatResult result = client.complete(loadedBoard(
                "Operator tech1 (apply=true): Start my assigned work",
                "[{\"id\":2,\"activityNumber\":\"WO-1001-1\",\"state\":\"ASSIGNED\",\"title\":\"Cabin kit\"}]",
                false));

        assertTrue(result.hasToolCalls());
        assertTrue(result.toolCalls().get(0).argumentsJson().contains("IN_PROGRESS"));
    }

    @Test
    void escalateIgnoresPeopleIds() {
        List<Map<String, Object>> messages = loadedBoard(
                "Operator manager (apply=true): Escalate overdue work orders",
                "[{\"id\":4,\"activityNumber\":\"WO-1002\",\"state\":\"PENDING\",\"title\":\"AOG pack\",\"priority\":\"AOG\",\"dueAt\":\"2020-01-01T00:00:00Z\"}]",
                true);
        messages.add(Map.of("role", "tool", "tool_call_id", "people", "content",
                "[{\"id\":99,\"username\":\"tech1\",\"fullName\":\"Omar\",\"roles\":[\"MAINTENANCE_TECHNICIAN\"]}]"));

        LlmClient.ChatResult result = client.complete(messages);

        assertTrue(result.hasToolCalls());
        for (LlmClient.ToolCall call : result.toolCalls()) {
            assertEquals("change_status", call.name());
            assertFalse(call.argumentsJson().contains("\"activityId\":99"));
            assertTrue(call.argumentsJson().contains("ESCALATED"));
        }
    }

    @Test
    void afterToolsItAnswersInPlainLanguage() {
        List<Map<String, Object>> messages = loadedBoard(
                "Operator manager (apply=false): What is overdue?",
                "[{\"id\":1,\"activityNumber\":\"WO-1\",\"state\":\"PENDING\",\"title\":\"Leak\"}]",
                false);
        messages.add(Map.of("role", "assistant", "content", "", "tool_calls", List.of(
                Map.of("id", "3", "type", "function", "function", Map.of("name", "assign_work_order", "arguments", "{}"))
        )));
        messages.add(Map.of("role", "tool", "tool_call_id", "3", "content",
                "{\"ok\":true,\"activityNumber\":\"WO-1\",\"state\":\"ASSIGNED\",\"assignedTo\":\"Omar Haddad\"}"));

        LlmClient.ChatResult result = client.complete(messages);

        assertFalse(result.hasToolCalls());
        assertFalse(AgentNarration.looksLikeJson(result.content()));
        assertTrue(result.content().contains("WO-1"));
        assertTrue(result.content().contains("Omar Haddad"));
        assertFalse(result.content().contains("{\""));
    }

    @Test
    void createOpensWorkOrderOnNamedAircraft() {
        List<Map<String, Object>> messages = loadedBoard(
                "Operator engineer (apply=true): Create a work order on A6-DREAM for pack 2 inop",
                "[]", false);
        LlmClient.ChatResult result = client.complete(messages);
        assertTrue(result.hasToolCalls());
        assertEquals("create_work_order", result.toolCalls().get(0).name());
        assertTrue(result.toolCalls().get(0).argumentsJson().contains("A6-DREAM"));
        assertTrue(result.toolCalls().get(0).argumentsJson().toLowerCase().contains("pack 2 inop"));
    }

    @Test
    void requestOnlyStripsOperatorPrefix() {
        assertEquals("Assign pending jobs to tech1",
                LocalFreeLlmClient.requestOnly("Operator manager (apply=true): Assign pending jobs to tech1"));
    }

    private List<Map<String, Object>> loadedBoard(String user, String workJson, boolean people) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", user));
        List<Map<String, Object>> calls = new ArrayList<>();
        calls.add(Map.of("id", "1", "type", "function", "function", Map.of("name", "dashboard", "arguments", "{}")));
        calls.add(Map.of("id", "2", "type", "function", "function", Map.of("name", "list_work_orders", "arguments", "{}")));
        if (people) {
            calls.add(Map.of("id", "p", "type", "function", "function", Map.of("name", "list_people", "arguments", "{}")));
        }
        if (user.toLowerCase().contains("start") || user.toLowerCase().contains("complete")) {
            calls.add(Map.of("id", "q", "type", "function", "function", Map.of("name", "list_my_queue", "arguments", "{}")));
        }
        if (user.toLowerCase().contains("create") || user.toLowerCase().contains("open a work")) {
            calls.add(Map.of("id", "a", "type", "function", "function", Map.of("name", "list_aircraft", "arguments", "{}")));
        }
        messages.add(Map.of("role", "assistant", "content", "", "tool_calls", calls));
        messages.add(Map.of("role", "tool", "tool_call_id", "1", "content",
                "{\"openActivities\":3,\"inProgress\":1,\"overdue\":1,\"awaitingQa\":1,\"aog\":[],\"dueSoon\":[]}"));
        messages.add(Map.of("role", "tool", "tool_call_id", "2", "content", workJson));
        if (people) {
            messages.add(Map.of("role", "tool", "tool_call_id", "p", "content",
                    "[{\"id\":7,\"username\":\"tech1\",\"fullName\":\"Omar Haddad\",\"roles\":[\"MAINTENANCE_TECHNICIAN\"]}]"));
        }
        if (user.toLowerCase().contains("create") || user.toLowerCase().contains("open a work")) {
            messages.add(Map.of("role", "tool", "tool_call_id", "a", "content",
                    "[{\"id\":4,\"registration\":\"A6-DREAM\",\"status\":\"AOG\",\"station\":\"AUH\"}]"));
        }
        return messages;
    }
}
