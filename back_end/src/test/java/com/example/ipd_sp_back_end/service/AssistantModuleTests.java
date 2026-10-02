package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssistantModuleTests {
    @Test
    void classificationCanBeReplacedWithoutChangingPromptsOrReplies() {
        AssistantIntentClassifier classifier = question -> AssistantIntent.CAPABILITY;
        AssistantService service = new AssistantService(classifier, new AssistantReplyCatalog(),
                new AssistantContextFormatter(), new AssistantPromptBuilder());
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("arbitrary intent supplied by another classifier");
        assertEquals(new AssistantReplyCatalog().capability(false), service.ask(request));
    }

    @Test
    void capabilityClassificationRetainsPriorityOverHealthKeywords() {
        AssistantIntentClassifier classifier = new RuleBasedAssistantIntentClassifier();
        assertEquals(AssistantIntent.CAPABILITY, classifier.classify("what can you do for my fitness?"));
        assertEquals(AssistantIntent.CAPABILITY, classifier.classify("你 能 为 我 做 什么？"));
        assertEquals(AssistantIntent.HEALTH, classifier.classify("健康运动计划"));
        assertEquals(AssistantIntent.OUT_OF_SCOPE, classifier.classify("write a poem"));
    }
}
