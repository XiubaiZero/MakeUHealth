package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.assistant.memory.AssistantContextBudget;
import com.example.ipd_sp_back_end.assistant.memory.MemorySnapshot;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

@Component @Primary
public class SemanticAssistantIntentClassifier implements AssistantIntentClassifier {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(SemanticAssistantIntentClassifier.class);
    private final RuleBasedAssistantIntentClassifier rules;
    private final ChatModelClient client;
    private final AssistantIntentProperties properties;
    private final ObjectMapper json=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final String system;
    public SemanticAssistantIntentClassifier(RuleBasedAssistantIntentClassifier rules,ChatModelClient client,AssistantIntentProperties properties) throws IOException {
        this.rules=rules;this.client=client;this.properties=properties;properties.validate();
        try(var stream=new ClassPathResource("prompts/intent-v1.txt").getInputStream()) {system=new String(stream.readAllBytes(),StandardCharsets.UTF_8);}
    }
    /** The legacy string contract is deliberately free of model calls. */
    @Override public AssistantIntent classify(String question) { return rules.classify(question); }
    @Override public AssistantIntentDecision classify(AssistantIntentInput input,AssistantCallDeadline deadline) {
        if (!"semantic".equals(properties.getMode()) || !client.isConfigured()) return AssistantIntentDecision.rules(rules.classify(input.question()));
        long start=System.nanoTime(); ChatCompletionResult result=null;
        var rounds=new ArrayList<MemorySnapshot.Round>(input.rounds().subList(Math.max(0,input.rounds().size()-properties.getRecentRounds()),input.rounds().size()));
        String summary=input.summary(); int used=rounds.size()*2;
        try {
            AssistantPrompt prompt=prompt(input,summary,rounds);
            while (AssistantContextBudget.estimate(prompt)>properties.getInputBudget() && !rounds.isEmpty()) {
                rounds.remove(0); prompt=prompt(input,summary,rounds);
            }
            if (AssistantContextBudget.estimate(prompt)>properties.getInputBudget() && !summary.isBlank()) {summary="";prompt=prompt(input,summary,rounds);}
            used=rounds.size()*2;
            if (AssistantContextBudget.estimate(prompt)>properties.getInputBudget()) return failure("input_budget",used,start,null);
            result=client.complete(prompt,new ChatCompletionOptions(properties.getTemperature(),properties.getMaxTokens(),deadline.timeout(properties.getTimeoutSeconds()),true));
            if ("length".equals(result.finishReason())) return failure("truncated",used,start,result);
            JsonNode node=json.readTree(result.text());
            if (node==null || !node.isObject() || node.size()!=1 || !node.path("intent").isTextual()) return failure("invalid_json",used,start,result);
            AssistantIntent intent=AssistantIntent.valueOf(node.get("intent").asText());
            return new AssistantIntentDecision(intent,"classified","",used,(System.nanoTime()-start)/1_000_000,result);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return failure(exception instanceof IllegalArgumentException ? "invalid_category" : "classification_failed",used,start,result);
        }
    }
    private AssistantPrompt prompt(AssistantIntentInput input,String summary,List<MemorySnapshot.Round> rounds) throws Exception {
        return new AssistantPrompt(system,json.writeValueAsString(Map.of("currentQuestion",input.question(),"language",Objects.toString(input.language(),"en"),"earlierSummary",summary,"recentRounds",rounds)));
    }
    private AssistantIntentDecision failure(String reason,int history,long start,ChatCompletionResult result) {
        long elapsed=(System.nanoTime()-start)/1_000_000;
        log.warn("Assistant intent degraded: reason={}, historyMessages={}, elapsedMillis={}",reason,history,elapsed);
        return new AssistantIntentDecision(null,"degraded",reason,history,elapsed,result);
    }
}
