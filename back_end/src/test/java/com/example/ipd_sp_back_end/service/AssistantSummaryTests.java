package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.assistant.memory.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssistantSummaryTests {
    MemorySnapshot snapshot() { return new MemorySnapshot(true,3,2,1,"旧摘要",0,List.of(new MemorySnapshot.Round(5,6,"训练计划","近期回答")),List.of(new MemorySnapshot.Round(1,2,"不吃海鲜","豆类建议"),new MemorySnapshot.Round(3,4,"每次半小时","训练建议")),3,List.of()); }
    @Test void summaryIsIncrementalImmutableAndUsesSeparateModelOptions() {
        var prompts=new ArrayList<AssistantPrompt>(); var options=new ArrayList<ChatCompletionOptions>();
        var client=new ChatModelClient() {
            public boolean isConfigured(){return true;} public String complete(AssistantPrompt p){throw new AssertionError();}
            public ChatCompletionResult complete(AssistantPrompt p,ChatCompletionOptions o){prompts.add(p);options.add(o);return new ChatCompletionResult("用户不吃海鲜，每次训练半小时。","test","stop",10,Map.of("prompt_tokens",100));}
        };
        var original=snapshot(); var prepared=new AssistantSummaryProcessor(client,new AssistantMemoryProperties()).prepare(original,"zh-CN");
        assertEquals("updated",prepared.status()); assertEquals(4,prepared.through()); assertEquals("旧摘要",original.summary());
        assertTrue(prompts.get(0).userPrompt().contains("不吃海鲜")); assertFalse(prompts.get(0).userPrompt().contains("近期回答"));
        assertEquals(0,options.get(0).temperature()); assertEquals(20,options.get(0).timeoutSeconds()); assertEquals(2048,options.get(0).maxTokens());
    }
    @Test void summaryFailureKeepsOldContextAndTooLargeBatchNeverClaimsCoverage() {
        var failing=new ChatModelClient(){public boolean isConfigured(){return true;} public String complete(AssistantPrompt p){throw new RuntimeException("offline");}};
        var processor=new AssistantSummaryProcessor(failing,new AssistantMemoryProperties()); var prepared=processor.prepare(snapshot(),"en");
        assertEquals("failed",prepared.status()); assertSame(snapshot().getClass(),prepared.context().getClass()); assertEquals("旧摘要",prepared.context().summary()); assertNull(prepared.update());
        var huge=new MemorySnapshot(true,0,0,0,"",0,List.of(),List.of(new MemorySnapshot.Round(1,2,"x".repeat(9000),"answer")),1,List.of());
        assertEquals("too_large",processor.prepare(huge,"en").status());
    }
    @Test void fixedRepliesAndLocalBranchesNeverGenerateSummary() {
        var calls=new ArrayList<AssistantPrompt>();
        var client=new ChatModelClient(){public boolean isConfigured(){return true;} public String complete(AssistantPrompt p){calls.add(p);return "unused";}};
        var engine=new JavaAssistantEngine(AssistantServiceTestSupport.service(client),new RuleBasedAssistantIntentClassifier());
        var request=new AssistantChatRequest();request.setMessage("你能做什么");request.setLanguage("zh-CN");
        assertEquals("fixed",engine.generate(new AssistantGenerationRequest(request,"api",snapshot())).source());
        request.setMessage("健康计划"); assertEquals("local",engine.generate(new AssistantGenerationRequest(request,"local",snapshot())).source()); assertTrue(calls.isEmpty());
    }
    @Test void completePipelineUsesAtMostOneSummaryAndOneEnglishRewrite() {
        var options=new ArrayList<ChatCompletionOptions>(); var prompts=new ArrayList<AssistantPrompt>();
        var client=new ChatModelClient(){
            public boolean isConfigured(){return true;} public String complete(AssistantPrompt p){throw new AssertionError();}
            public ChatCompletionResult complete(AssistantPrompt p,ChatCompletionOptions o){options.add(o);prompts.add(p);return new ChatCompletionResult(switch(options.size()){case 1->"No seafood. Thirty minutes.";case 2->"保持运动。";default->"Keep exercising.";},"test","stop",5,Map.of());}
        };
        var settings=new AssistantMemoryProperties(); var model=new DeepSeekProperties();
        var service=new AssistantService(new RuleBasedAssistantIntentClassifier(),new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),client,new AssistantResponseProcessor(),new AssistantContextBudget(settings),model,new AssistantSummaryProcessor(client,settings));
        var request=new AssistantChatRequest();request.setMessage("Expand the second suggestion");request.setLanguage("en");
        var answer=service.askWithMemory(request,snapshot()); assertEquals("Keep exercising.",answer.answer()); assertEquals(3,options.size());
        assertTrue(prompts.get(1).userPrompt().contains("Thirty minutes")); assertEquals(60,options.get(1).timeoutSeconds());
    }
}
