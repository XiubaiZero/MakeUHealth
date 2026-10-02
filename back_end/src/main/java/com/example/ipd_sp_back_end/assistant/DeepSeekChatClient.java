package com.example.ipd_sp_back_end.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class DeepSeekChatClient implements ChatModelClient {
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Autowired
    public DeepSeekChatClient(DeepSeekProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    public DeepSeekChatClient(DeepSeekProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public String complete(AssistantPrompt prompt) throws Exception {
        String systemPrompt = prompt.systemPrompt();
        String userPrompt = prompt.userPrompt();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("temperature", 0.4);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("DeepSeek API returned status: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String answer = root.path("choices").path(0).path("message").path("content").asText("");
        if (answer == null || answer.isBlank()) {
            throw new RuntimeException("DeepSeek API returned empty answer.");
        }

        return answer;
    }

}
