package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.assistant.memory.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.repository.AssistantConversationRepository;
import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Opt-in synthetic evaluation driver. Not a JUnit test; ordinary builds never call a paid model. */
public final class AssistantMemoryEvaluation {
    private static final ObjectMapper json=new ObjectMapper();
    public static void main(String[] args) throws Exception {
        try(var reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))) {
            String line;
            while((line=reader.readLine())!=null) {
                var input=json.readTree(line);long started=System.nanoTime();var output=new LinkedHashMap<String,Object>();output.put("id",input.path("id").asText());
                try { output.putAll(evaluate(input));output.put("ok",true); }
                catch(Exception exception) { output.put("ok",false);output.put("error",exception.getClass().getSimpleName()+": "+exception.getMessage()); }
                output.put("turnMillis",(System.nanoTime()-started)/1_000_000);System.out.println("RESULT "+json.writeValueAsString(output));System.out.flush();
            }
        }
    }
    private static Map<String,Object> evaluate(JsonNode input) throws Exception {
        var config=input.path("config");var props=new AssistantMemoryProperties();
        if(config.has("recentRounds")) props.setRecentRounds(config.get("recentRounds").asInt());
        if(config.has("inputBudget")) props.setInputBudget(config.get("inputBudget").asInt());
        if(config.has("contextWindow")) props.setContextWindow(config.get("contextWindow").asInt());
        if(config.has("summaryMaxTokens")) props.setSummaryMaxTokens(config.get("summaryMaxTokens").asInt());
        if(config.has("extractionMaxTokens")) props.setExtractionMaxTokens(config.get("extractionMaxTokens").asInt());
        if(config.has("auxiliaryTimeoutSeconds")) props.setAuxiliaryTimeoutSeconds(config.get("auxiliaryTimeoutSeconds").asInt());
        if(config.has("summaryTemperature")) props.setSummaryTemperature(config.get("summaryTemperature").asDouble());
        if(config.has("extractionTemperature")) props.setExtractionTemperature(config.get("extractionTemperature").asDouble());
        if(config.has("capacity")) props.setCapacity(config.get("capacity").asInt());props.validate();
        var modelProps=new DeepSeekProperties();modelProps.setApiKey("evaluation-local-gateway");modelProps.setBaseUrl("http://127.0.0.1:14179");
        if(config.has("temperature")) modelProps.setTemperature(config.get("temperature").asDouble());
        if(config.has("maxTokens")) modelProps.setMaxTokens(config.get("maxTokens").asInt());
        var client=new DeepSeekChatClient(modelProps);var summary=new AssistantSummaryProcessor(client,props);var extractor=new AssistantMemoryExtractor(client,props);
        String language=input.path("language").asText("zh-CN"),mode=input.path("mode").asText("answer");
        if("extract".equals(mode)) {
            var sources=new ArrayList<AssistantMemoryExtractor.Source>();int index=0;
            for(var message:input.path("userMessages")) {String text=message.asText();sources.add(new AssistantMemoryExtractor.Source("u"+(index++),text,AssistantMemoryExtractor.hash(text)));}
            var result=extractor.extract(sources,language);return Map.of("candidates",result.candidates(),"calls",List.of(result.completion()));
        }
        var data=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        try {
            var jdbc=new JdbcTemplate(data);var tx=new TransactionTemplate(new DataSourceTransactionManager(data));jdbc.execute("CREATE TABLE auth_user(id INT PRIMARY KEY)");jdbc.update("INSERT INTO auth_user VALUES(1)");
            for(String file:List.of("assistant-schema.sql","assistant-memory-schema.sql")) {
                String schema=new String(new ClassPathResource(file).getInputStream().readAllBytes(),StandardCharsets.UTF_8).replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
                for(String sql:schema.split(";")) if(!sql.isBlank()) jdbc.execute(sql);
            }
            var memory=new AssistantMemoryService(jdbc,tx,props);var repo=new AssistantConversationRepository(jdbc);String conversation=UUID.randomUUID().toString(),questionId=UUID.randomUUID().toString();
            var snapshot=tx.execute(s->{
                memory.lock(1);repo.create(1,conversation,"Synthetic evaluation",null);memory.ensure(1,conversation);
                for(var round:input.path("rounds")) {repo.append(conversation,UUID.randomUUID().toString(),"user",round.path("question").asText(),language,"imported",null,true);repo.append(conversation,UUID.randomUUID().toString(),"assistant",round.path("answer").asText(),language,"imported",null,true);}
                var personal=new AssistantPersonalMemoryService(memory);
                for(var fact:input.path("facts")) personal.add(1,new AssistantPersonalMemoryService.Change(personal.state(1).settings().revision(),"other",fact.asText(),true,null,null));
                if(input.has("summary")) jdbc.update("UPDATE assistant_memory_conversation SET summary=? WHERE conversation_id=?",input.get("summary").asText(),conversation);
                if(input.path("memoryOff").asBoolean()) memory.toggle(1,conversation,0,false);
                repo.append(conversation,questionId,"user",input.path("question").asText(),language,"user",null,false);
                return memory.snapshot(1,conversation,questionId);
            });
            if("summary".equals(mode)) {
                var prepared=summary.prepare(snapshot,language);return Map.of("summary",prepared.context().summary(),"status",prepared.status(),"through",prepared.through(),"calls",prepared.calls());
            }
            var service=new AssistantService(new RuleBasedAssistantIntentClassifier(),new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),client,new AssistantResponseProcessor(),new AssistantContextBudget(props),modelProps,summary);
            var request=new AssistantChatRequest();request.setMessage(input.path("question").asText());request.setLanguage(language);
            if(input.has("context")) request.setContext(json.convertValue(input.get("context"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){}));
            if(input.has("constraints")) request.setConstraints(json.convertValue(input.get("constraints"),new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}));
            service.validateMemoryInput(request,snapshot);
            var result=service.askWithMemory(request,snapshot);
            return Map.of("answer",result.answer(),"historyMessages",result.historyMessages(),"factsCount",snapshot.facts().size(),"summaryStatus",result.summaryStatus(),"calls",result.calls(),"reduced",result.reduced());
        } finally { new JdbcTemplate(data).execute("SHUTDOWN"); }
    }
}
