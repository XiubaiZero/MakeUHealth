package com.example.ipd_sp_back_end.assistant;

import org.springframework.stereotype.Component;
@Component
public class AssistantReplyCatalog {
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


    public String capability(boolean chinese) { return chinese ? "我可以结合你保存的健康数据，解释健康指标，提供饮食、运动、睡眠和恢复建议，比较健身目标进度，并帮助制定日常计划。你可以问：根据最新健康数据，我本周应重点关注什么？我不提供医学诊断或急救服务；持续不适或指标异常时，请咨询医生。" : CAPABILITY_RESPONSE; }
    public String outOfScope(boolean chinese) { return chinese ? "我只能回答身体健康、饮食营养和健身计划相关的问题，请提出这些范围内的问题。" : "I can only answer questions about body health, diet nutrition, and fitness planning. Please ask a question in this scope."; }
    public String missingConfiguration(boolean chinese) { return chinese ? "健康助手暂未配置，请稍后再试。" : "Assistant API key is missing on the server. Please configure DEEPSEEK_API_KEY."; }
}
