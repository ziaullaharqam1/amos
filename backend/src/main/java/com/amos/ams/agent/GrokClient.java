package com.amos.ams.agent;

import com.amos.ams.config.AppProperties;
import com.amos.ams.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GrokClient {
    private final AppProperties properties;
    private final RestClient http;

    public GrokClient(AppProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(120_000);
        this.http = RestClient.builder()
                .baseUrl(properties.getGrok().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public LlmClient.ChatResult complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        AppProperties.Grok grok = properties.getGrok();
        if (!grok.isConfigured()) {
            throw ApiException.serviceUnavailable(
                    "Grok is not configured. Set environment variable GROK_API_KEY to your xAI key from https://console.x.ai/");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", grok.getModel());
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("temperature", 0.1);
        body.put("reasoning_effort", "low");
        try {
            JsonNode root = http.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + grok.getApiKey())
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        String err = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw ApiException.serviceUnavailable("Grok HTTP " + res.getStatusCode().value() + ": " + err);
                    })
                    .body(JsonNode.class);
            if (root == null || !root.has("choices") || root.get("choices").isEmpty()) {
                throw ApiException.serviceUnavailable("Grok returned an empty response");
            }
            JsonNode message = root.get("choices").get(0).path("message");
            String content = message.path("content").asText(null);
            List<LlmClient.ToolCall> calls = new ArrayList<>();
            for (JsonNode call : message.path("tool_calls")) {
                calls.add(new LlmClient.ToolCall(
                        call.path("id").asText(),
                        call.path("function").path("name").asText(),
                        call.path("function").path("arguments").asText("{}")
                ));
            }
            return new LlmClient.ChatResult(content, calls);
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw ApiException.serviceUnavailable("Grok request failed: " + e.getMessage());
        }
    }
}
