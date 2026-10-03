package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.assistant.memory.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Opt-in evaluation through a budget-enforcing localhost gateway. Never run by JUnit. */
public final class AssistantIntentEvaluation {
    static final ObjectMapper JSON=new ObjectMapper();
    public static void main(String[] args) throws Exception {
        try(var reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null){var input=JSON.readTree(line);var output=new LinkedHashMap<String,Object>();long start=System.nanoTime();
                output.put("id",input.path("id").asText());
                try{output.putAll(evaluate(input));output.put("ok",true);}catch(Exception e){output.put("ok",false);output.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}
                output.put("turnMillis",(System.nanoTime()-start)/1_000_000);System.out.println("RESULT "+JSON.writeValueAsString(output));System.out.flush();
            }
        }
    }
    static Map<String,Object> evaluate(JsonNode input) throws Exception {
        var model=new DeepSeekProperties();model.setApiKey("evaluation-local-gateway");model.setBaseUrl("http://127.0.0.1:14180");
        var client=new DeepSeekChatClient(model);var settings=new AssistantIntentProperties();settings.setMode("semantic");
        AssistantIntentClassifier classifier="old".equals(input.path("chain").asText())?new RuleBasedAssistantIntentClassifier():new SemanticAssistantIntentClassifier(new RuleBasedAssistantIntentClassifier(),client,settings);
        var rounds=new ArrayList<MemorySnapshot.Round>();long sequence=1;
        for(var round:input.path("rounds")){rounds.add(new MemorySnapshot.Round(sequence++,sequence++,round.path("question").asText(),round.path("answer").asText()));}
        boolean enabled=!input.path("memoryOff").asBoolean();
        var snapshot=new MemorySnapshot(enabled,0,0,0,enabled?input.path("summary").asText(""):"",0,enabled?rounds:List.of(),List.of(),rounds.size(),List.of());
        String question=input.path("question").asText(),language=input.path("language").asText("en");
        if("classify".equals(input.path("mode").asText())){
            var result=classifier.classify(AssistantIntentInput.from(question,language,snapshot),AssistantCallDeadline.unlimited());
            return Map.of("intent",result.metadata());
        }
        var memoryProps=new AssistantMemoryProperties();
        var service=new AssistantService(classifier,new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),client,new AssistantResponseProcessor(),new AssistantContextBudget(memoryProps),model,new AssistantSummaryProcessor(client,memoryProps));
        var request=new AssistantChatRequest();request.setMessage(question);request.setLanguage(language);
        request.setContext(JSON.convertValue(input.path("context"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){}));
        var result=service.askWithMemory(request,snapshot);
        return Map.of("answer",result.answer(),"intent",result.intent().metadata(),"historyMessages",result.historyMessages(),"usedModel",result.usedModel(),"callPurposes",result.callPurposes(),"calls",result.calls());
    }
}
