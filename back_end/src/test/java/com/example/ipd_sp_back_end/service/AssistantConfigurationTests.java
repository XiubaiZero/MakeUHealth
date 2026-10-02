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
}
