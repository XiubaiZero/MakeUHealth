package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AssistantService {

    private static final List<String> SCOPE_KEYWORDS_EN = List.of(
            "health", "body", "diet", "nutrition", "food", "meal", "weight", "blood pressure", "heart",
            "sleep", "exercise", "calorie", "protein", "fat", "carb", "bmi", "sugar", "glucose", "hydration",
            "fitness", "workout", "training", "gym", "strength", "cardio", "muscle", "muscle gain", "fat loss",
            "fat reduction", "weight loss", "exercise plan", "fitness plan", "daily routine"
    );

    private static final List<String> SCOPE_KEYWORDS_ZH = List.of(
            "\u5065\u5eb7", "\u8eab\u4f53", "\u996e\u98df", "\u8425\u517b", "\u4f53\u91cd", "\u8840\u538b",
            "\u5fc3\u7387", "\u8840\u7cd6", "\u7761\u7720", "\u8fd0\u52a8", "\u953b\u70bc", "\u5065\u8eab",
            "\u51cf\u8102", "\u589e\u808c", "\u4f53\u8102", "\u5851\u5f62", "\u8bad\u7ec3", "\u4f5c\u606f",
            "\u4ee3\u8c22", "\u996e\u98df\u8ba1\u5212", "\u8bad\u7ec3\u8ba1\u5212", "\u5065\u8eab\u8ba1\u5212"
    );

    private static final List<String> CAPABILITY_HINTS_EN = List.of(
            "what can you do", "how can you help", "what can you help me with",
            "your capabilities", "your functions", "what do you support", "what can i ask"
    );

    private static final List<String> CAPABILITY_HINTS_ZH = List.of(
            "\u4f60\u80fd\u505a\u4ec0\u4e48", "\u4f60\u53ef\u4ee5\u505a\u4ec0\u4e48", "\u80fd\u5e2e\u6211\u4ec0\u4e48",
            "\u4f60\u6709\u4ec0\u4e48\u529f\u80fd", "\u80fd\u4e3a\u6211\u505a\u4e9b\u4ec0\u4e48", "\u4f60\u652f\u6301\u4ec0\u4e48"
    );

    private static final List<String> CAPABILITY_EXTRA_HINTS_EN = List.of(
            "can you help me", "could you help me", "what features do you have",
            "what functions do you have", "what are your capabilities", "how can i use you", "how to use you"
    );

    private static final List<String> CAPABILITY_EXTRA_HINTS_ZH = List.of(
            "\u4f60\u80fd\u4e3a\u6211\u505a\u4ec0\u4e48", "\u4f60\u53ef\u4ee5\u4e3a\u6211\u505a\u4ec0\u4e48",
            "\u4f60\u80fd\u5e2e\u6211\u505a\u4ec0\u4e48", "\u4f60\u53ef\u4ee5\u5e2e\u6211\u505a\u4ec0\u4e48",
            "\u4f60\u53ef\u4ee5\u600e\u4e48\u5e2e\u6211", "\u6211\u53ef\u4ee5\u95ee\u4f60\u4ec0\u4e48",
            "\u80fd\u95ee\u4f60\u4ec0\u4e48", "\u4f60\u6709\u54ea\u4e9b\u529f\u80fd", "\u4f60\u6709\u54ea\u4e9b\u80fd\u529b",
            "\u4f60\u652f\u6301\u54ea\u4e9b"
    );

    private static final List<Pattern> CAPABILITY_EXTRA_REGEX_EN = List.of(
            Pattern.compile("\\b(can|could)\\b[\\s\\S]{0,20}\\byou\\b[\\s\\S]{0,20}\\b(help|assist|support)\\b[\\s\\S]{0,20}\\bme\\b"),
            Pattern.compile("\\bwhat\\b[\\s\\S]{0,30}\\b(features?|functions?|capabilities)\\b[\\s\\S]{0,30}\\bdo\\b[\\s\\S]{0,30}\\byou\\b[\\s\\S]{0,30}\\b(have|support)\\b")
    );

    private static final List<Pattern> CAPABILITY_EXTRA_REGEX_ZH = List.of(
            Pattern.compile("(?:\\u4f60|\\u60a8|ai|\\u52a9\\u624b|\\u667a\\u80fd\\u52a9\\u624b)[\\s\\S]{0,12}(?:\\u80fd|\\u53ef\\u4ee5|\\u4f1a|\\u652f\\u6301|\\u5e2e)[\\s\\S]{0,20}(?:\\u505a\\u4ec0\\u4e48|\\u505a\\u54ea\\u4e9b|\\u4ec0\\u4e48\\u529f\\u80fd|\\u54ea\\u4e9b\\u529f\\u80fd|\\u4ec0\\u4e48\\u80fd\\u529b|\\u652f\\u6301\\u4ec0\\u4e48|\\u53ef\\u4ee5\\u95ee\\u4ec0\\u4e48)"),
            Pattern.compile("(?:\\u80fd|\\u53ef\\u4ee5|\\u4f1a)[\\s\\S]{0,6}(?:\\u4e3a\\u6211|\\u5e2e\\u6211)[\\s\\S]{0,12}(?:\\u505a\\u4ec0\\u4e48|\\u505a\\u54ea\\u4e9b|\\u63d0\\u4f9b\\u4ec0\\u4e48)")
    );

    private static final List<String> CAPABILITY_SUBJECT_MARKERS_ZH = List.of(
            "\u4f60", "\u60a8", "ai", "\u52a9\u624b", "\u667a\u80fd\u52a9\u624b"
    );

    private static final List<String> CAPABILITY_SELF_REF_MARKERS_ZH = List.of(
            "\u4e3a\u6211", "\u5e2e\u6211", "\u6211\u53ef\u4ee5\u95ee\u4f60", "\u95ee\u4f60", "\u5411\u4f60"
    );

    private static final List<String> CAPABILITY_ABILITY_MARKERS_ZH = List.of(
            "\u80fd", "\u53ef\u4ee5", "\u4f1a", "\u652f\u6301", "\u5e2e", "\u534f\u52a9"
    );

    private static final List<String> CAPABILITY_OBJECT_MARKERS_ZH = List.of(
            "\u4e3a\u6211\u505a\u4ec0\u4e48", "\u4e3a\u6211\u505a\u4e9b\u4ec0\u4e48", "\u5e2e\u6211\u505a\u4ec0\u4e48",
            "\u53ef\u4ee5\u600e\u4e48\u5e2e\u6211", "\u505a\u54ea\u4e9b", "\u54ea\u4e9b\u529f\u80fd", "\u4ec0\u4e48\u529f\u80fd",
            "\u6709\u4ec0\u4e48\u529f\u80fd", "\u6709\u54ea\u4e9b\u529f\u80fd", "\u6709\u54ea\u4e9b\u80fd\u529b",
            "\u652f\u6301\u4ec0\u4e48", "\u652f\u6301\u54ea\u4e9b", "\u80fd\u529b", "\u53ef\u4ee5\u95ee\u4ec0\u4e48",
            "\u80fd\u95ee\u4ec0\u4e48", "\u95ee\u4f60\u4ec0\u4e48", "\u80fd\u5e2e\u6211\u4ec0\u4e48", "\u63d0\u4f9b\u4ec0\u4e48\u5e2e\u52a9"
    );

    private static final String CAPABILITY_RESPONSE = """
            I can help you with health and fitness guidance based on your saved data.

            What I can do:
            1. Explain your profile and latest health indicators (BP, FBG, heart rate, oxyhemoglobin) in plain language.
            2. Give diet and hydration suggestions based on your food logs and goals.
            3. Give training and routine suggestions for muscle gain, weight loss, and fat loss plans.
            4. Compare your goal progress and suggest weekly adjustments.
            5. Suggest practical daily plans that combine meals, workouts, sleep, and recovery.

            Try asking:
            1. "Based on my latest data, what should I focus on this week?"
            2. "Create a daily routine for my fat-loss plan."
            3. "How should I adjust training for my weight-loss goal?"
            4. "Compare my muscle-gain and fat-loss progress and suggest priorities."
            5. "Give me a 7-day checklist for diet, workout, and sleep."

            Limitations:
            1. I do not provide diagnosis or emergency care.
            2. For persistent symptoms or abnormal values, consult a licensed clinician.
            """;

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

    public String ask(AssistantChatRequest request) {
        boolean chinese = "zh-CN".equals(request.getLanguage());
        String question = request.getMessage() == null ? "" : request.getMessage().trim();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        if (isCapabilityQuestion(question)) {
            return chinese ? "我可以结合你保存的健康数据，解释健康指标，提供饮食、运动、睡眠和恢复建议，比较健身目标进度，并帮助制定日常计划。你可以问：根据最新健康数据，我本周应重点关注什么？我不提供医学诊断或急救服务；持续不适或指标异常时，请咨询医生。" : CAPABILITY_RESPONSE;
        }

        if (!isInScope(question)) {
            return chinese ? "我只能回答身体健康、饮食营养和健身计划相关的问题，请提出这些范围内的问题。" : "I can only answer questions about body health, diet nutrition, and fitness planning. Please ask a question in this scope.";
        }

        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            return chinese ? "健康助手暂未配置，请稍后再试。" : "Assistant API key is missing on the server. Please configure DEEPSEEK_API_KEY.";
        }

        String contextText = formatContext(request.getContext());
        String constraintText = formatConstraints(request.getConstraints());

        String systemPrompt = "You are a private health assistant. "
                + "Only answer questions about body health, diet nutrition, and fitness planning. "
                + "Always use the given user context if available. "
                + (chinese ? "Respond in Simplified Chinese. " : "Respond in English only, even when the user writes in another language. Never output Chinese or any non-English language. ")
                + "Do not provide diagnosis. Suggest seeking licensed clinicians for abnormal or persistent symptoms.";

        String userPrompt = "User question:\n" + question + "\n\n"
                + "User context:\n" + contextText + "\n\n"
                + "Additional constraints:\n" + constraintText;

        try {
            String answer = callDeepSeek(systemPrompt, userPrompt).trim();

            if (!chinese && containsCjk(answer)) {
                answer = callDeepSeek(
                        "Rewrite the following assistant answer into clear natural English only. "
                                + "Keep the original meaning and safety tone. "
                                + "Do not add new medical facts.",
                        answer
                ).trim();
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

    private boolean isInScope(String question) {
        String normalized = question.toLowerCase();
        boolean matchedEnglish = SCOPE_KEYWORDS_EN.stream().anyMatch(normalized::contains);
        if (matchedEnglish) {
            return true;
        }
        return SCOPE_KEYWORDS_ZH.stream().anyMatch(question::contains);
    }

    private boolean isCapabilityQuestion(String question) {
        String normalized = question.toLowerCase();
        boolean matchedEnglish = CAPABILITY_HINTS_EN.stream().anyMatch(normalized::contains);
        if (matchedEnglish) {
            return true;
        }
        boolean matchedZh = CAPABILITY_HINTS_ZH.stream().anyMatch(question::contains);
        if (matchedZh) {
            return true;
        }

        boolean matchedExtraEnHint = CAPABILITY_EXTRA_HINTS_EN.stream().anyMatch(normalized::contains);
        if (matchedExtraEnHint) {
            return true;
        }
        boolean matchedExtraEnRegex = CAPABILITY_EXTRA_REGEX_EN.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
        if (matchedExtraEnRegex) {
            return true;
        }

        String normalizedZh = normalizeIntentText(question);
        boolean matchedExtraZhHint = CAPABILITY_EXTRA_HINTS_ZH.stream().anyMatch(normalizedZh::contains);
        if (matchedExtraZhHint) {
            return true;
        }
        boolean matchedExtraZhRegex = CAPABILITY_EXTRA_REGEX_ZH.stream().anyMatch(pattern -> pattern.matcher(normalizedZh).find());
        if (matchedExtraZhRegex) {
            return true;
        }

        boolean hasSubject = CAPABILITY_SUBJECT_MARKERS_ZH.stream().anyMatch(normalizedZh::contains);
        boolean hasSelfReference = CAPABILITY_SELF_REF_MARKERS_ZH.stream().anyMatch(normalizedZh::contains);
        boolean hasAbilityVerb = CAPABILITY_ABILITY_MARKERS_ZH.stream().anyMatch(normalizedZh::contains);
        boolean hasCapabilityObject = CAPABILITY_OBJECT_MARKERS_ZH.stream().anyMatch(normalizedZh::contains);

        return hasCapabilityObject && hasAbilityVerb && (hasSubject || hasSelfReference);
    }

    private String normalizeIntentText(String question) {
        return question.toLowerCase()
                .replaceAll("[\\s,.!?;:'\"()\\[\\]{}<>`~@#$%^&*_+=|\\\\/，。！？；：、“”‘’（）【】《》…—-]+", "");
    }

    private boolean containsCjk(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CJK_PATTERN.matcher(text).find();
    }

    private String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "No user context provided.";
        }

        StringBuilder builder = new StringBuilder();
        context.forEach((key, value) -> builder.append(key).append(": ").append(value).append('\n'));
        return builder.toString().trim();
    }

    private String formatConstraints(List<String> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return "None";
        }

        StringBuilder builder = new StringBuilder();
        for (String constraint : constraints) {
            builder.append("- ").append(constraint).append('\n');
        }
        return builder.toString().trim();
    }
}
