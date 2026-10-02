package com.example.ipd_sp_back_end.assistant;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
@Component
public class RuleBasedAssistantIntentClassifier implements AssistantIntentClassifier {
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


    @Override
    public AssistantIntent classify(String question) {
        if (isCapabilityQuestion(question)) return AssistantIntent.CAPABILITY;
        return isInScope(question) ? AssistantIntent.HEALTH : AssistantIntent.OUT_OF_SCOPE;
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


}
