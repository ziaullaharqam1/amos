package com.amos.ams.agent;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    ChatResult complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools);

    record ToolCall(String id, String name, String argumentsJson) {}

    record ChatResult(String content, List<ToolCall> toolCalls) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
