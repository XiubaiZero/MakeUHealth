package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AssistantLanguageTests {
    private AssistantChatRequest request(String language, String message) {
        AssistantChatRequest request = new AssistantChatRequest();
        request.setLanguage(language);
        request.setMessage(message);
        return request;
    }

    @Test
    void capabilityAndScopeRepliesFollowTheSelectedLanguage() {
        AssistantService service = new AssistantService();
        assertTrue(service.ask(request("zh-CN", "你能做什么")).contains("健康数据"));
        assertTrue(service.ask(request("en", "what can you do")).contains("health and fitness"));
        assertTrue(service.ask(request("zh-CN", "write a poem")).contains("我只能回答"));
        assertTrue(service.ask(request("en", "write a poem")).startsWith("I can only answer"));
    }

    @Test
    void missingConfigurationKeepsEnglishCompatibilityAndOffersAChineseReply() {
        AssistantService service = new AssistantService();
        assertTrue(service.ask(request(null, "fitness plan")).contains("API key is missing"));
        assertEquals("健康助手暂未配置，请稍后再试。", service.ask(request("zh-CN", "健康运动计划")));
    }

    @Test
    void chineseAnswerIsNotRewrittenIntoEnglish() throws Exception {
        AtomicReference<JsonNode> payload = new AtomicReference<>();
        ObjectMapper json = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            payload.set(json.readTree(exchange.getRequestBody()));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"请保持均衡饮食与规律运动。\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AssistantService service = new AssistantService();
            ReflectionTestUtils.setField(service, "deepseekApiKey", "test-only-key");
            ReflectionTestUtils.setField(service, "deepseekBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "deepseekModel", "test-model");
            assertEquals("请保持均衡饮食与规律运动。", service.ask(request("zh-CN", "健康运动计划")));
            String systemPrompt = payload.get().path("messages").get(0).path("content").asText();
            assertTrue(systemPrompt.contains("Respond in Simplified Chinese."));
            assertFalse(systemPrompt.contains("Never output Chinese"));
        } finally { server.stop(0); }
    }
}
