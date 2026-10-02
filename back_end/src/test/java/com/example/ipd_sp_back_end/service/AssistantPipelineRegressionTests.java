package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.controller.AssistantController;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.dto.ApiErrorResponse;
import com.example.ipd_sp_back_end.assistant.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.net.InetSocketAddress;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AssistantPipelineRegressionTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private AssistantChatRequest input(String language, String message) {
        var request = new AssistantChatRequest(); request.setLanguage(language); request.setMessage(message); return request;
    }
    @Test void blankInputKeepsIts400Response() {
        for (String question : new String[]{null, "", " \n "}) {
            var response = new AssistantController(AssistantServiceTestSupport.unconfigured()).chat(input("zh-CN", question));
            assertEquals(400, response.getStatusCode().value());
            assertEquals("Message cannot be empty.", ((ApiErrorResponse)response.getBody()).getMessage());
        }
    }
    @Test void earlyRepliesNeverCallModel() throws Exception {
        try (var model = new FakeModel(200, "unused")) {
            var service = AssistantServiceTestSupport.configured(model.url());
            assertTrue(service.ask(input("en", "what can you do")).contains("What I can do:"));
            assertTrue(service.ask(input("zh-CN", "你能做什么")).contains("健康数据"));
            assertTrue(service.ask(input("en", "write a poem")).startsWith("I can only answer"));
            assertTrue(service.ask(input("zh-CN", "write a poem")).startsWith("我只能回答"));
            assertEquals(0, model.calls.get());
        }
    }
    @Test void requestRetainsContextConstraintsAndModelSettings() throws Exception {
        try (var model = new FakeModel(200, "  Stay active.  ")) {
            var request = input(null, "  fitness plan  ");
            Map<String,Object> context = new LinkedHashMap<>(); context.put("age",30); context.put("goals",List.of("muscle_gain","fat_loss"));
            request.setContext(context); request.setConstraints(List.of("Use saved data.","Keep it short."));
            assertEquals("Stay active.", AssistantServiceTestSupport.configured(model.url()).ask(request));
            var payload = model.payloads.get(0);
            assertEquals("test-model",payload.path("model").asText()); assertEquals(0.4,payload.path("temperature").asDouble());
            assertEquals(2,payload.path("messages").size());
            assertTrue(payload.path("messages").get(0).path("content").asText().contains("Respond in English only"));
            assertEquals("User question:\nfitness plan\n\nUser context:\nage: 30\ngoals: [muscle_gain, fat_loss]\n\nAdditional constraints:\n- Use saved data.\n- Keep it short.",payload.path("messages").get(1).path("content").asText());
            assertEquals("Bearer test-only-key",model.authorization); assertEquals(1,model.calls.get());
        }
    }
    @Test void absentContextKeepsOriginalPlaceholders() throws Exception {
        try (var model = new FakeModel(200,"均衡饮食。")) {
            assertEquals("均衡饮食。",AssistantServiceTestSupport.configured(model.url()).ask(input("zh-CN","健康计划")));
            String prompt=model.payloads.get(0).path("messages").get(1).path("content").asText();
            assertTrue(prompt.contains("User context:\nNo user context provided.")); assertTrue(prompt.endsWith("Additional constraints:\nNone"));
            assertEquals(1,model.calls.get());
        }
    }
    @Test void englishRewriteOccursAtMostOnce() throws Exception {
        for (String rewritten : List.of("Stay active.","仍有中文。")) {
            try (var model = new FakeModel(200,"保持运动。",rewritten)) {
                assertEquals(rewritten,AssistantServiceTestSupport.configured(model.url()).ask(input("en","fitness plan")));
                assertEquals(2,model.calls.get());
                var rewrite=model.payloads.get(1).path("messages");
                assertTrue(rewrite.get(0).path("content").asText().startsWith("Rewrite the following assistant answer"));
                assertEquals("保持运动。",rewrite.get(1).path("content").asText());
            }
        }
    }
    @Test void upstreamFailuresKeep500Response() throws Exception {
        try (var model=new FakeModel(503,"offline")) { assertModelFailure(model); }
        try (var model=new FakeModel(200,"")) { assertModelFailure(model); }
        try (var model=new FakeModel(200,"answer")) { model.rawBody="not-json"; assertModelFailure(model); }
    }
    @Test void failedEnglishRewriteKeeps500AndDoesNotReturnTheOriginalAnswer() {
        AtomicInteger calls = new AtomicInteger();
        ChatModelClient client = new ChatModelClient() {
            public boolean isConfigured() { return true; }
            public String complete(AssistantPrompt prompt) {
                if (calls.incrementAndGet() == 1) return "保持运动。";
                throw new RuntimeException("rewrite unavailable");
            }
        };
        var response = new AssistantController(AssistantServiceTestSupport.service(client)).chat(input("en", "fitness plan"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("Assistant request failed: rewrite unavailable", ((ApiErrorResponse)response.getBody()).getMessage());
        assertEquals(2, calls.get());
    }
    private void assertModelFailure(FakeModel model) {
        var response=new AssistantController(AssistantServiceTestSupport.configured(model.url())).chat(input("en","fitness plan"));
        assertEquals(500,response.getStatusCode().value());
        assertTrue(((ApiErrorResponse)response.getBody()).getMessage().startsWith("Assistant request failed:"));
    }
    @Test void timeoutKeepsErrorAnd60SecondLimit() throws Exception {
        var client=Mockito.mock(HttpClient.class); List<HttpRequest> requests=new ArrayList<>();
        when(client.send(any(HttpRequest.class),Mockito.<HttpResponse.BodyHandler<String>>any())).thenAnswer(invocation->{ requests.add(invocation.getArgument(0)); throw new HttpTimeoutException("simulated timeout"); });
        var response=new AssistantController(AssistantServiceTestSupport.withHttpClient(client)).chat(input("en","fitness plan"));
        assertEquals(500,response.getStatusCode().value()); assertTrue(((ApiErrorResponse)response.getBody()).getMessage().contains("simulated timeout"));
        assertEquals(Duration.ofSeconds(60),requests.get(0).timeout().orElseThrow());
    }
    @Test void concurrentContextsDoNotMix() throws Exception {
        try (var model=new FakeModel(200,"answer")) {
            var service=AssistantServiceTestSupport.configured(model.url()); var executor=Executors.newFixedThreadPool(2);
            try {
                var futures=List.of("A","B").stream().map(owner->executor.submit(()->{var request=input("en","fitness plan "+owner); request.setContext(Map.of("owner",owner)); return service.ask(request);})).toList();
                for(var future:futures) assertEquals("answer",future.get());
                assertEquals(2,model.calls.get());
                for(var payload:model.payloads){String prompt=payload.path("messages").get(1).path("content").asText(); String owner=prompt.contains("fitness plan A")?"A":"B"; assertTrue(prompt.contains("owner: "+owner)); assertFalse(prompt.contains("owner: "+(owner.equals("A")?"B":"A")));}
            } finally {executor.shutdownNow();}
        }
    }
    private static final class FakeModel implements AutoCloseable {
        final AtomicInteger calls=new AtomicInteger(); final List<JsonNode> payloads=new CopyOnWriteArrayList<>(); final HttpServer server;
        volatile String authorization,rawBody;
        FakeModel(int status,String... answers) throws Exception {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/chat/completions",exchange->{
                int index=calls.getAndIncrement(); payloads.add(JSON.readTree(exchange.getRequestBody())); authorization=exchange.getRequestHeaders().getFirst("Authorization");
                String body=rawBody!=null?rawBody:JSON.writeValueAsString(Map.of("choices",List.of(Map.of("message",Map.of("content",answers[Math.min(index,answers.length-1)])))));
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
                exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            }); server.start();
        }
        String url(){return "http://127.0.0.1:"+server.getAddress().getPort();}
        @Override public void close(){server.stop(0);}
    }
}
