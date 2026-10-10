package com.example.ipd_sp_back_end.assistant;

import org.springframework.stereotype.Component;
@Component
public class AssistantPromptBuilder {
    private final String answerTemplate = AssistantPromptResources.languageTemplate("prompts/answer-v1.txt");
    private final String rewriteInstructions = AssistantPromptResources.load("prompts/english-rewrite-v1.txt");

    public AssistantPrompt build(String question, boolean chinese, String contextText, String constraintText) {
        String systemPrompt = answerTemplate.replace(AssistantPromptResources.LANGUAGE_INSTRUCTION,
                chinese ? "Respond in Simplified Chinese. " : "Respond in English only, even when the user writes in another language. Never output Chinese or any non-English language. ");

        String userPrompt = "User question:\n" + question + "\n\n"
                + "User context:\n" + contextText + "\n\n"
                + "Additional constraints:\n" + constraintText;

        return new AssistantPrompt(systemPrompt, userPrompt);
    }

    public AssistantPrompt rewriteEnglish(String answer) {
        return new AssistantPrompt(
                        rewriteInstructions,
                        answer
        );
    }
}
