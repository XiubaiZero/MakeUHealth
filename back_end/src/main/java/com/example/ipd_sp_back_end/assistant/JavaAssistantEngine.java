package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.service.AssistantService;
import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Preserves the browser's local replies while making the server the answer owner. */
@Component
public class JavaAssistantEngine implements AssistantEngine {
    private final AssistantService assistant;
    private final AssistantIntentClassifier classifier;
    public JavaAssistantEngine(AssistantService assistant, AssistantIntentClassifier classifier) { this.assistant = assistant; this.classifier = classifier; }
    private boolean capability(String question) {
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (classifier.classify(question) == AssistantIntent.CAPABILITY || classifier.classify(normalized) == AssistantIntent.CAPABILITY) return true;
        return Pattern.compile("\\bwhat\\b[\\s\\S]{0,30}\\bcan\\b[\\s\\S]{0,30}\\byou\\b[\\s\\S]{0,30}\\b(do|help|support)\\b|\\bhow\\b[\\s\\S]{0,30}\\bcan\\b[\\s\\S]{0,30}\\byou\\b[\\s\\S]{0,30}\\bhelp\\b|\\bwhat\\b[\\s\\S]{0,30}\\bcan\\b[\\s\\S]{0,30}\\bi\\b[\\s\\S]{0,30}\\bask\\b[\\s\\S]{0,30}\\byou\\b").matcher(normalized).find();
    }
    @Override public AssistantAnswer generate(AssistantChatRequest request, String mode) {
        boolean chinese = "zh-CN".equals(request.getLanguage());
        if (capability(request.getMessage())) return new AssistantAnswer(capabilityReply(chinese), "fixed");
        if ("local".equals(mode)) return new AssistantAnswer(local(request), "local");
        try {
            String answer = assistant.ask(request);
            return answer == null || answer.isBlank() ? new AssistantAnswer(local(request), "local") : new AssistantAnswer(answer, "api");
        } catch (RuntimeException exception) {
            return new AssistantAnswer(local(request) + (chinese ? " （服务暂不可用，当前使用本地参考回答。）" : " (API is unavailable, currently using local fallback.)"), "fallback");
        }
    }
    public String capabilityReply(boolean chinese) {
        if (chinese) return "我可以结合你保存的健康数据提供健康和健身建议。\n\n我能帮助你：\n1. 解释个人档案及血压、血糖、心率、血氧等指标。\n2. 根据饮食记录和目标提供饮食、饮水建议。\n3. 为增肌、减重和减脂提供训练与作息建议。\n4. 比较目标进度，建议每周调整。\n5. 制定结合饮食、运动、睡眠和恢复的日常安排。\n\n可以试着问：\n根据最新健康数据，我本周应重点关注什么？\n请为我的减脂目标制定 7 天运动与饮食计划。\n\n我不提供医学诊断或急救服务。持续不适或指标异常时，请咨询医生。";
        return """
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
                2. For persistent symptoms or abnormal values, consult a licensed clinician.""";
    }
    private String value(Map<?, ?> context, String key) { return Objects.toString(context.get(key), "-"); }
    private String goalType(Object type) {
        if (type == null || "".equals(type)) return "none";
        return switch (type.toString()) { case "weight_loss" -> "weight loss"; case "muscle_gain" -> "muscle gain"; case "fat_loss" -> "fat loss"; default -> type.toString(); };
    }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> goals(Map<String, Object> context) {
        Object raw = context.get("allGoalSnapshots");
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }
    public String local(AssistantChatRequest request) {
        boolean chinese = "zh-CN".equals(request.getLanguage());
        String question = request.getMessage();
        if (capability(question)) return capabilityReply(chinese);
        if (classifier.classify(question) == AssistantIntent.OUT_OF_SCOPE) return chinese ? "我只能回答身体健康、饮食营养和健身计划相关的问题，请提出这些范围内的问题。" : "I can only answer questions about body health, diet nutrition, and fitness planning. Please ask a question in this scope.";
        Map<String, Object> context = request.getContext() == null ? Map.of() : request.getContext();
        if (!Boolean.TRUE.equals(context.get("hasProfile"))) return chinese ? "请先完善个人档案并提交至少一条健康记录，以便提供个性化健康建议。" : "Please complete your profile and submit at least one health record first. Then I can provide personalized health suggestions.";
        var goals = goals(context);
        if (chinese) {
            String joined = goals.stream().map(goal -> ("muscle_gain".equals(goal.get("goalType")) ? "增肌" : "fat_loss".equals(goal.get("goalType")) ? "减脂" : "减重") + "：当前 " + value(goal,"currentValue") + "，目标 " + value(goal,"targetValue") + "，进度 " + value(goal,"latestProgressPercentage") + "%").collect(Collectors.joining("；"));
            String gender = Objects.toString(context.get("gender"), "");
            gender = switch (gender) { case "male" -> "男"; case "female" -> "女"; default -> gender; };
            return "个人档案：" + value(context,"age") + " 岁，" + gender + "，身高 " + value(context,"height") + " cm，体重 " + value(context,"weight") + " kg。\n最新健康指标：血压 " + value(context,"latestSystolic") + " / " + value(context,"latestDiastolic") + " mmHg，空腹血糖 " + value(context,"latestFbg") + " mmol/L，心率 " + value(context,"latestHeartRate") + " 次/分钟，血氧 " + value(context,"latestOxyhemoglobin") + "%。\n最近 7 天有 " + value(context,"last7DaysFoodCount") + " 条饮食记录。" + (joined.isEmpty() ? "" : "\n健身目标：" + joined) + "\n建议保持均衡饮食、循序渐进训练、充足睡眠，并每周记录变化。如指标持续异常或身体不适，请咨询医生。";
        }
        String fitness = goals.isEmpty()
                ? "Fitness goal: " + goalType(context.get("activeGoalType")) + " (status: " + value(context,"activeGoalStatus") + ", current: " + value(context,"activeGoalCurrentValue") + ", target: " + value(context,"activeGoalTargetValue") + ", remaining weeks: " + value(context,"remainingGoalWeeks") + ")."
                : "Fitness goals: " + goals.stream().map(goal -> goalType(goal.get("goalType")) + " [status: " + value(goal,"status") + ", current: " + value(goal,"currentValue") + ", target: " + value(goal,"targetValue") + ", progress: " + value(goal,"latestProgressPercentage") + "%]").collect(Collectors.joining("; ")) + ".";
        return "Profile: age " + value(context,"age") + ", gender " + value(context,"gender") + ", height " + value(context,"height") + " cm, weight " + value(context,"weight") + " kg. Latest health: BP " + value(context,"latestSystolic") + " / " + value(context,"latestDiastolic") + " mmHg, FBG " + value(context,"latestFbg") + " mmol/L, heart rate " + value(context,"latestHeartRate") + " bpm, oxyhemoglobin " + value(context,"latestOxyhemoglobin") + "%. Diet logs in last 7 days: " + value(context,"last7DaysFoodCount") + ". " + fitness + " Based on your question \"" + question + "\", combine balanced meals, structured training progression, recovery sleep, and weekly tracking. If any metric is consistently abnormal, please consult a licensed clinician.";
    }
}
