package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.assistant.memory.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AssistantSemanticIntentTests {
    static final ObjectMapper JSON=new ObjectMapper();
    static class Model implements ChatModelClient {
        boolean configured=true; String category="HEALTH", raw; int failures;
        final List<AssistantPrompt> prompts=new CopyOnWriteArrayList<>();
        final List<ChatCompletionOptions> options=new CopyOnWriteArrayList<>();
        public boolean isConfigured(){return configured;}
        public String complete(AssistantPrompt p){throw new AssertionError("Per-call options required");}
        public ChatCompletionResult complete(AssistantPrompt p,ChatCompletionOptions o) throws Exception {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());prompts.add(p);options.add(o);
            if(o.json() && failures>0){failures--;throw new HttpTimeoutException("simulated");}
            String text=o.json()?(raw==null?"{\"intent\":\""+category+"\"}":raw):"健康建议。";
            return new ChatCompletionResult(text,"test","stop",1,Map.of("prompt_tokens",20,"completion_tokens",5));
        }
    }
    AssistantIntentProperties settings(){var p=new AssistantIntentProperties();p.setMode("semantic");return p;}
    AssistantIntentClassifier classifier(Model m,AssistantIntentProperties p) throws Exception{return new SemanticAssistantIntentClassifier(new RuleBasedAssistantIntentClassifier(),m,p);}
    AssistantService service(Model m,AssistantIntentProperties p) throws Exception {
        return new AssistantService(classifier(m,p),new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),m,new AssistantResponseProcessor());
    }
    AssistantChatRequest request(String text){var r=new AssistantChatRequest();r.setMessage(text);r.setLanguage("zh-CN");return r;}
    MemorySnapshot memory(boolean enabled){return new MemorySnapshot(enabled,0,0,0,"健康讨论摘要",0,List.of(new MemorySnapshot.Round(1,2,"饮食计划","均衡饮食"),new MemorySnapshot.Round(3,4,"训练安排","散步"),new MemorySnapshot.Round(5,6,"睡眠","规律作息"),new MemorySnapshot.Round(7,8,"头晕","询问细节")),List.of(),4,List.of("偏好豆类"));}
    @Test void allFiveCategoriesClassifyOnceAndOnlyHealthCallsAnswer() throws Exception {
        for(var category:AssistantIntent.values()) {
            var m=new Model();m.category=category.name();var result=service(m,settings()).askWithMemory(request("Can you help me lose weight?"),MemorySnapshot.empty());
            assertEquals(category,result.intent().intent());assertEquals(category==AssistantIntent.HEALTH?2:1,m.prompts.size());
            assertEquals(category==AssistantIntent.HEALTH,result.usedModel());assertEquals("intent",result.callPurposes().get(0));
            assertEquals(0,m.options.get(0).temperature());assertEquals(256,m.options.get(0).maxTokens());assertEquals(10,m.options.get(0).timeoutSeconds());
            if(result.usedModel()){assertEquals(.4,m.options.get(1).temperature());assertEquals(4096,m.options.get(1).maxTokens());assertEquals(60,m.options.get(1).timeoutSeconds());}
        }
    }
    @Test void semanticEngineDoesNotRepeatCapabilityRulesAndStoresPurposeMetadata() throws Exception {
        var m=new Model();var c=classifier(m,settings());var s=new AssistantService(c,new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),m,new AssistantResponseProcessor());
        var result=new JavaAssistantEngine(s,c).generate(new AssistantGenerationRequest(request("Can you help me lose weight?"),"api",MemorySnapshot.empty()));
        assertEquals("api",result.source());assertEquals(2,m.options.size());assertNotNull(result.memory().get("intent"));
        assertEquals(false,result.memory().get("enabled"));
    }
    @Test void fixedClassificationDoesNotReportMainMemoryUsage() throws Exception {
        var m=new Model();m.category="CAPABILITY";var c=classifier(m,settings());var s=new AssistantService(c,new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),m,new AssistantResponseProcessor());
        var result=new JavaAssistantEngine(s,c).generate(new AssistantGenerationRequest(request("你能做什么"),"api",memory(true)));
        assertEquals("fixed",result.source());assertEquals(false,result.memory().get("enabled"));assertEquals(0,result.memory().get("longTermCount"));assertEquals(1,m.options.size());
    }
    @Test void invalidResultsAndTimeoutGoStraightToAnswerWithoutRuleRejection() throws Exception {
        for(String raw:List.of("", "not-json", "[]", "{}", "{\"intent\":\"INVALID\"}","{\"intent\":3}","{\"intent\":\"HEALTH\",\"extra\":1}","{\"intent\":\"HEALTH\"} {}")) {
            var m=new Model();m.raw=raw;var result=service(m,settings()).askWithMemory(request("最近总是头晕，该怎么办？"),MemorySnapshot.empty());
            assertTrue(result.intent().degraded());assertTrue(result.usedModel());assertEquals(2,m.options.size());
        }
        var m=new Model();m.failures=1;assertTrue(service(m,settings()).askWithMemory(request("I feel dizzy"),MemorySnapshot.empty()).usedModel());assertEquals(2,m.options.size());
    }
    @Test void historyIsLimitedAndMemoryOffRemovesSummaryAndRounds() throws Exception {
        for(boolean enabled:List.of(true,false)) {
            var m=new Model();var r=request("展开第二条");r.setContext(Map.of("privateProfile","not for classification"));
            var result=service(m,settings()).askWithMemory(r,memory(enabled));var data=JSON.readTree(m.prompts.get(0).userPrompt());
            assertEquals(enabled?3:0,data.path("recentRounds").size());assertEquals(enabled?"健康讨论摘要":"",data.path("earlierSummary").asText());
            assertFalse(m.prompts.get(0).userPrompt().contains("privateProfile"));assertFalse(m.prompts.get(0).userPrompt().contains("偏好豆类"));
            if(enabled){assertEquals(8,result.historyMessages());assertTrue(m.prompts.get(1).userPrompt().contains("偏好豆类"));}
        }
    }
    @Test void budgetDropsWholeOldRoundsThenSummaryWithoutTruncatingQuestion() throws Exception {
        var m=new Model();var p=settings();p.setInputBudget(3000);
        var snapshot=new MemorySnapshot(true,0,0,0,"x".repeat(6000),0,List.of(new MemorySnapshot.Round(1,2,"old".repeat(1000),"answer"),new MemorySnapshot.Round(3,4,"recent","reply")),List.of(),2,List.of());
        classifier(m,p).classify(AssistantIntentInput.from("CURRENT QUESTION","en",snapshot),AssistantCallDeadline.unlimited());
        var data=JSON.readTree(m.prompts.get(0).userPrompt());assertEquals("CURRENT QUESTION",data.path("currentQuestion").asText());assertTrue(data.path("earlierSummary").asText().isEmpty());assertTrue(data.path("recentRounds").isEmpty());
        var huge=new Model();var result=service(huge,settings()).askWithMemory(request("头晕"+"x".repeat(9000)),MemorySnapshot.empty());
        assertEquals("input_budget",result.intent().reason());assertEquals(1,huge.options.size());assertFalse(huge.options.get(0).json());
    }
    @Test void localRulesMissingKeyAndDisabledModeNeverCallClassifierModel() throws Exception {
        var m=new Model();var p=settings();var c=classifier(m,p);var s=new AssistantService(c,new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),m,new AssistantResponseProcessor());
        new JavaAssistantEngine(s,c).generate(new AssistantGenerationRequest(request("健康计划"),"local",memory(true)));assertTrue(m.options.isEmpty());
        p.setMode("rules");s.ask(request("你能做什么"));assertTrue(m.options.isEmpty());
        p.setMode("semantic");m.configured=false;assertTrue(s.ask(request("健康计划")).contains("暂未配置"));assertTrue(m.options.isEmpty());
    }
    @Test void blankInputStopsBeforeAnyClassification() throws Exception {
        var m=new Model();var s=service(m,settings());for(var text:List.of(""," ","\t\n","　")) assertThrows(IllegalArgumentException.class,()->s.ask(request(text)));assertTrue(m.options.isEmpty());
    }
    @Test void synchronousCallsHaveRemainingTurnDeadlineAndExpiredDeadlineDoesNotCall() throws Exception {
        var m=new Model();service(m,settings()).ask(request("健康计划"));assertTrue(m.options.get(1).timeoutSeconds()<=55);
        assertThrows(IllegalStateException.class,()->AssistantCallDeadline.afterSeconds(0).timeout(60));
    }
    @Test void concurrentRequestsCannotShareClassificationContext() throws Exception {
        var m=new Model();var s=service(m,settings());var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->s.askWithMemory(request("健康 A"),MemorySnapshot.empty()));var b=pool.submit(()->s.askWithMemory(request("健康 B"),MemorySnapshot.empty()));assertTrue(a.get().usedModel());assertTrue(b.get().usedModel());
            assertEquals(2,m.prompts.stream().filter(p->p.systemPrompt().contains("intent router")).count());
            for(var p:m.prompts){assertFalse(p.userPrompt().contains("健康 A") && p.userPrompt().contains("健康 B"));}
        }finally{pool.shutdownNow();}
    }
}
