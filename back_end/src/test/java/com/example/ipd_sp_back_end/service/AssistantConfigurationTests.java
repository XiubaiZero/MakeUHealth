package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssistantConfigurationTests {
    @Test
    void springWiresTheCompleteChainAndBindsExistingConfigurationNames() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "assistant.deepseek.base-url", "http://127.0.0.1:1",
                    "assistant.deepseek.model", "test-model",
                    "assistant.deepseek.api-key", "test-only-key")));
            context.scan("com.example.ipd_sp_back_end.assistant");
            context.registerBean(org.springframework.jdbc.core.JdbcTemplate.class,()->org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class));
            context.registerBean(org.springframework.transaction.support.TransactionTemplate.class,()->org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class));
            context.register(AssistantService.class);
            context.refresh();
            var properties = context.getBean(DeepSeekProperties.class);
            assertEquals("http://127.0.0.1:1", properties.getBaseUrl());
            assertEquals("test-model", properties.getModel());
            assertEquals("test-only-key", properties.getApiKey());
            assertNotNull(context.getBean(AssistantService.class));
            assertTrue(context.getBean(ChatModelClient.class).isConfigured());
            HttpClient http = (HttpClient)ReflectionTestUtils.getField(context.getBean(ChatModelClient.class), "httpClient");
            assertEquals(Duration.ofSeconds(20), http.connectTimeout().orElseThrow());
        }
    }

    @Test
    void defaultsRetainExistingModelAndMissingKeyBehavior() {
        var properties = new DeepSeekProperties();
        assertEquals("https://api.deepseek.com", properties.getBaseUrl());
        assertEquals("deepseek-flash", properties.getModel());
        assertFalse(new DeepSeekChatClient(properties).isConfigured());
    }
    @Test void documentedMemoryEnvironmentVariablesActuallyBind() {
        try(var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.SystemEnvironmentPropertySource("systemEnvironment",Map.of(
                "ASSISTANT_MEMORY_RECENTROUNDS","20","ASSISTANT_MEMORY_INPUTBUDGET","16384","ASSISTANT_MEMORY_SUMMARYMAXTOKENS","512","ASSISTANT_MEMORY_EXTRACTIONMAXTOKENS","1024","ASSISTANT_MEMORY_AUXILIARYTIMEOUTSECONDS","40","ASSISTANT_MEMORY_CAPACITY","50","ASSISTANT_DEEPSEEK_MAXTOKENS","8192","ASSISTANT_DEEPSEEK_TEMPERATURE","0.7")));
            context.register(AssistantConfiguration.class);context.refresh();
            var properties=context.getBean(com.example.ipd_sp_back_end.assistant.memory.AssistantMemoryProperties.class);
            assertEquals(20,properties.getRecentRounds());assertEquals(16384,properties.getInputBudget());assertEquals(512,properties.getSummaryMaxTokens());assertEquals(1024,properties.getExtractionMaxTokens());assertEquals(40,properties.getAuxiliaryTimeoutSeconds());assertEquals(50,properties.getCapacity());
            assertEquals(8192,context.getBean(DeepSeekProperties.class).getMaxTokens());assertEquals(0.7,context.getBean(DeepSeekProperties.class).getTemperature());
        }
    }
}
