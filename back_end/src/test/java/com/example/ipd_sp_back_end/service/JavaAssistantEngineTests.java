package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JavaAssistantEngineTests {
    @Test void migratedRepliesMatchBrowserFixtures() throws Exception {
        var json = new ObjectMapper();
        var fixtures = json.readTree(new ClassPathResource("assistant-reply-fixtures.json").getInputStream());
        var engine = new JavaAssistantEngine(mock(AssistantService.class), new RuleBasedAssistantIntentClassifier());
        for (var fixture : fixtures) {
            var request = new AssistantChatRequest();
            request.setMessage(fixture.path("question").asText()); request.setLanguage(fixture.path("language").asText());
            request.setContext(json.convertValue(fixture.path("context"), new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() { }));
            assertEquals(fixture.path("answer").asText(), engine.generate(request, "local").answer(), request.getMessage() + ":" + request.getLanguage());
        }
    }
    @Test void capabilityAndLocalModeNeverCallModelAndFallbackIsMarked() {
        var assistant = mock(AssistantService.class);
        var engine = new JavaAssistantEngine(assistant, new RuleBasedAssistantIntentClassifier());
        var request = new AssistantChatRequest();
        request.setMessage("what can you actually do"); request.setLanguage("en"); request.setContext(Map.of());
        assertTrue(engine.generate(request, "local").answer().contains("What I can do:"));
        request.setMessage("运动计划"); request.setLanguage("zh-CN");
        request.setContext(Map.of("hasProfile", true, "age", 20, "gender", "male", "last7DaysFoodCount", 0, "allGoalSnapshots", List.of(Map.of("goalType", "muscle_gain", "currentValue", 0))));
        var local = engine.generate(request, "local");
        assertTrue(local.answer().contains("20 岁，男")); assertTrue(local.answer().contains("当前 0"));
        verifyNoInteractions(assistant);
        when(assistant.ask(any())).thenThrow(new RuntimeException("simulated model failure"));
        var fallback = engine.generate(request, "api");
        assertEquals("fallback", fallback.source()); assertTrue(fallback.answer().contains("本地参考回答"));
        request.setLanguage("en"); assertTrue(engine.generate(request, "api").answer().contains("local fallback"));
    }
}
