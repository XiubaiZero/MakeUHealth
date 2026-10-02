package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.assistant.*;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private final AssistantIntentClassifier intentClassifier;
    private final AssistantReplyCatalog replyCatalog;
    private final AssistantContextFormatter contextFormatter;
    private final AssistantPromptBuilder promptBuilder;
    private final ChatModelClient modelClient;
    private final AssistantResponseProcessor responseProcessor;

    public AssistantService(AssistantIntentClassifier intentClassifier, AssistantReplyCatalog replyCatalog,
                            AssistantContextFormatter contextFormatter, AssistantPromptBuilder promptBuilder,
                            ChatModelClient modelClient, AssistantResponseProcessor responseProcessor) {
        this.intentClassifier = intentClassifier;
        this.replyCatalog = replyCatalog;
        this.contextFormatter = contextFormatter;
        this.promptBuilder = promptBuilder;
        this.modelClient = modelClient;
        this.responseProcessor = responseProcessor;
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
        if (!modelClient.isConfigured()) return replyCatalog.missingConfiguration(chinese);

        String contextText = contextFormatter.formatContext(request.getContext());
        String constraintText = contextFormatter.formatConstraints(request.getConstraints());

        AssistantPrompt prompt = promptBuilder.build(question, chinese, contextText, constraintText);

        try {
            String answer = responseProcessor.clean(modelClient.complete(prompt));

            if (responseProcessor.needsEnglishRewrite(chinese, answer)) {
                AssistantPrompt rewritePrompt = promptBuilder.rewriteEnglish(answer);
                answer = responseProcessor.clean(modelClient.complete(rewritePrompt));
            }

            return answer;
        } catch (Exception exception) {
            throw new RuntimeException("Assistant request failed: " + exception.getMessage(), exception);
        }
    }

}
