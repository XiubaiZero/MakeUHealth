package com.example.ipd_sp_back_end.assistant;

import org.springframework.stereotype.Component;
@Component
public class AssistantPromptBuilder {
    public AssistantPrompt build(String question, boolean chinese, String contextText, String constraintText) {
        String systemPrompt = "You are a private health assistant. "
                + "Only answer questions about body health, diet nutrition, and fitness planning. "
                + "Always use the given user context if available. "
                + (chinese ? "Respond in Simplified Chinese. " : "Respond in English only, even when the user writes in another language. Never output Chinese or any non-English language. ")
                + "Do not provide diagnosis. Suggest seeking licensed clinicians for abnormal or persistent symptoms.";

        String userPrompt = "User question:\n" + question + "\n\n"
                + "User context:\n" + contextText + "\n\n"
                + "Additional constraints:\n" + constraintText;

        return new AssistantPrompt(systemPrompt, userPrompt);
    }

    public AssistantPrompt rewriteEnglish(String answer) {
        return new AssistantPrompt(
                        "Rewrite the following assistant answer into clear natural English only. "
                                + "Keep the original meaning and safety tone. "
                                + "Do not add new medical facts.",
                        answer
        );
    }
}
