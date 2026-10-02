package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.assistant.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class AssistantService {

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\p{IsHan}]");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${assistant.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${assistant.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${assistant.deepseek.api-key:}")
    private String deepseekApiKey;

    private final AssistantIntentClassifier intentClassifier;
    private final AssistantReplyCatalog replyCatalog;
    private final AssistantContextFormatter contextFormatter;
    private final AssistantPromptBuilder promptBuilder;

    public AssistantService(AssistantIntentClassifier intentClassifier, AssistantReplyCatalog replyCatalog,
                            AssistantContextFormatter contextFormatter, AssistantPromptBuilder promptBuilder) {
        this.intentClassifier = intentClassifier;
        this.replyCatalog = replyCatalog;
        this.contextFormatter = contextFormatter;
        this.promptBuilder = promptBuilder;
    }

    public String ask(AssistantChatRequest request) {
        boolean chinese = "zh-CN".equals(request.getLanguage());
        String question = request.getMessage() == null ? "" : request.getMessage().trim();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        AssistantIntent intent = intentClassifier.classify(question);
        if (intent == AssistantIntent.CAPABILITY) return replyCatalog.capability(chinese);
        if (intent == AssistantIntent.OUT_OF_SCOPE) return replyCatalog.outOfScope(chinese);
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) return replyCatalog.missingConfiguration(chinese);

        String contextText = contextFormatter.formatContext(request.getContext());
        String constraintText = contextFormatter.formatConstraints(request.getConstraints());

        AssistantPrompt prompt = promptBuilder.build(question, chinese, contextText, constraintText);

        try {
            String answer = callDeepSeek(prompt.systemPrompt(), prompt.userPrompt()).trim();

            if (!chinese && containsCjk(answer)) {
                AssistantPrompt rewritePrompt = promptBuilder.rewriteEnglish(answer);
                answer = callDeepSeek(rewritePrompt.systemPrompt(), rewritePrompt.userPrompt()).trim();
            }

            return answer;
        } catch (Exception exception) {
            throw new RuntimeException("Assistant request failed: " + exception.getMessage(), exception);
        }
    }

    private String callDeepSeek(String systemPrompt, String userPrompt) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", deepseekModel);
        payload.put("temperature", 0.4);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(deepseekBaseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + deepseekApiKey)
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

    private boolean containsCjk(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CJK_PATTERN.matcher(text).find();
    }

}
