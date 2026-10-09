package com.amos.ams.agent;

import com.amos.ams.config.AppProperties;
import com.amos.ams.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoutingLlmClient implements LlmClient {
    private final AppProperties properties;
    private final GrokClient grok;
    private final LocalFreeLlmClient local;

    public RoutingLlmClient(AppProperties properties, GrokClient grok, ObjectMapper mapper) {
        this.properties = properties;
        this.grok = grok;
        this.local = new LocalFreeLlmClient(mapper);
    }

    @Override
    public ChatResult complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        if (properties.getGrok().isFree()) {
            return local.complete(messages);
        }
        try {
            return grok.complete(messages, tools);
        } catch (ApiException e) {
            if (isCreditBlock(e.getMessage())) {
                return local.complete(messages);
            }
            throw e;
        }
    }

    static boolean isCreditBlock(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        return m.contains("permission-denied") || m.contains("doesn't have any credits")
                || m.contains("does not have any credits") || m.contains("no credits");
    }
}
