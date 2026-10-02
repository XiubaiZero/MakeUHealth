package com.example.ipd_sp_back_end.assistant.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.Executor;

/** Durable, idempotent explicit extraction; neither HTTP nor parsing holds database locks. */
@Service
public class AssistantMemoryExtractionService {
    private final AssistantMemoryService memory;
    private final AssistantPersonalMemoryService personal;
    private final AssistantMemoryExtractor extractor;
    private final Executor executor;
    private final ObjectMapper json=new ObjectMapper();
    public AssistantMemoryExtractionService(AssistantMemoryService memory,AssistantPersonalMemoryService personal,AssistantMemoryExtractor extractor,@Qualifier("assistantMemoryExecutor") Executor executor) { this.memory=memory;this.personal=personal;this.extractor=extractor;this.executor=executor; }
    public record Request(String requestId,Long expectedRevision,String language) { }
    public record Job(String id,String conversationId,String requestId,String status,String result,String error) { }
    public record Payload(long accountRevision,long settingsRevision,long contentRevision,String language,List<AssistantMemoryExtractor.Source> sources) { }
    private Job find(int account,String request) {
        var rows=memory.jdbc.query("SELECT * FROM assistant_memory_extraction WHERE account_id=? AND request_id=?",(r,n)->new Job(r.getString("id"),r.getString("conversation_id"),r.getString("request_id"),r.getString("status"),r.getString("result_json"),r.getString("error_message")),account,request);
        return rows.isEmpty()?null:rows.get(0);
    }
    public Job get(int account,String conversation,String request) {
        return personal.transaction(account,()->{memory.ensure(account,conversation);var job=find(account,request);if(job==null || !conversation.equals(job.conversationId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Extraction task not found.");return job;});
    }
    public Job create(int account,String conversation,Request request) {
        try { UUID.fromString(request.requestId()); } catch(Exception e) { throw new IllegalArgumentException("A valid request ID is required."); }
        boolean[] created={false};
        var job=personal.transaction(account,()->{
            memory.ensure(account,conversation);
            var previous=find(account,request.requestId());
            if(previous!=null) { if(!conversation.equals(previous.conversationId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Request ID already belongs to another conversation.");return previous; }
            Long revision=memory.jdbc.queryForObject("SELECT revision FROM assistant_conversation WHERE id=? AND account_id=? FOR UPDATE",Long.class,conversation,account);
            if(request.expectedRevision()==null || !revision.equals(request.expectedRevision())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Conversation changed on another device. Refresh and try again.");
            var setting=memory.settings(account,conversation);var accountSettings=personal.settings(account);
            if(!setting.enabled() || !accountSettings.enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Enable conversation and long-term memory before extracting.");
            if(!extractor.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"The model is not configured for memory extraction.");
            if(memory.jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_extraction WHERE account_id=? AND status IN ('queued','running')",Integer.class,account)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Memory extraction is already running.");
            if(memory.jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_item WHERE account_id=? AND status='pending'",Integer.class,account)>=50) throw new ResponseStatusException(HttpStatus.CONFLICT,"Memory capacity reached. Delete or replace an entry first.");
            var sources=memory.jdbc.query("SELECT id,content FROM assistant_message WHERE conversation_id=? AND role='user' ORDER BY sequence_no DESC LIMIT 12",(r,n)->new AssistantMemoryExtractor.Source(r.getString("id"),r.getString("content"),AssistantMemoryExtractor.hash(r.getString("content"))),conversation);
            if(sources.isEmpty()) throw new IllegalArgumentException("There are no user messages to extract.");
            sources=new ArrayList<>(sources);Collections.reverse(sources);
            var payload=new Payload(accountSettings.revision(),setting.revision(),setting.contentRevision(),"zh-CN".equals(request.language())?"zh-CN":"en",List.copyOf(sources));
            String raw=encode(payload);
            if(AssistantContextBudget.estimate(raw)+2048>memory.properties.getInputBudget()) throw new IllegalArgumentException("Messages are too large for memory extraction.");
            memory.jdbc.update("INSERT INTO assistant_memory_extraction(id,account_id,conversation_id,request_id,status,payload_json) VALUES(?,?,?,?,'queued',?)",UUID.randomUUID().toString(),account,conversation,request.requestId(),raw);created[0]=true;return find(account,request.requestId());
        });
        if(created[0]) {
            try { executor.execute(()->run(account,conversation,job)); }
            catch(RuntimeException e) { fail(account,job.id(),"Extraction is busy. Please retry."); }
        }
        return job;
    }
    private String encode(Object value) { try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);} }
    private void run(int account,String conversation,Job job) {
        try {
            String raw=personal.transaction(account,()->{
                int claimed=memory.jdbc.update("UPDATE assistant_memory_extraction SET status='running',started_at=CURRENT_TIMESTAMP WHERE id=? AND account_id=? AND status='queued'",job.id(),account);
                return claimed==0?null:memory.jdbc.queryForObject("SELECT payload_json FROM assistant_memory_extraction WHERE id=?",String.class,job.id());
            });
            if(raw==null) return;
            var payload=json.readValue(raw,Payload.class);
            var result=extractor.extract(payload.sources(),payload.language()); // outside transactions
            personal.transaction(account,()->{
                if(memory.jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_extraction WHERE id=? AND status='running'",Integer.class,job.id())!=1) return null;
                var setting=memory.settings(account,conversation);var accountSetting=personal.settings(account);
                boolean valid=setting.enabled() && accountSetting.enabled() && setting.revision()==payload.settingsRevision() && setting.contentRevision()==payload.contentRevision() && accountSetting.revision()==payload.accountRevision();
                for(var source:payload.sources()) {
                    var contents=memory.jdbc.queryForList("SELECT content FROM assistant_message WHERE id=? AND conversation_id=? AND role='user'",String.class,source.messageId(),conversation);
                    valid &= contents.size()==1 && AssistantMemoryExtractor.hash(contents.get(0)).equals(source.hash());
                }
                if(!valid) { memory.jdbc.update("UPDATE assistant_memory_extraction SET status='failed',payload_json=NULL,error_message=? WHERE id=?","Memory changed while extracting. Please retry.",job.id());return null; }
                var ids=personal.saveCandidates(account,conversation,payload.sources(),result);var completion=result.completion();
                String info=encode(Map.of("candidateIds",ids,"model",completion.model(),"finishReason",completion.finishReason(),"usage",completion.usage(),"elapsedMillis",completion.elapsedMillis()));
                memory.jdbc.update("UPDATE assistant_memory_extraction SET status='completed',payload_json=NULL,result_json=? WHERE id=?",info,job.id());return null;
            });
        } catch(Exception e) { fail(account,job.id(),"Memory extraction failed. Please retry."); }
    }
    private void fail(int account,String id,String error) {
        try { personal.transaction(account,()->{memory.jdbc.update("UPDATE assistant_memory_extraction SET status='failed',payload_json=NULL,error_message=? WHERE id=? AND account_id=? AND status IN ('queued','running')",error,id,account);return null;}); }
        catch(RuntimeException ignored) { /* Recovery marks stalled tasks; never repeat a paid call automatically. */ }
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() { memory.transactions.execute(s->{memory.jdbc.update("UPDATE assistant_memory_extraction SET status='failed',payload_json=NULL,error_message='Extraction interrupted. Please retry.' WHERE status IN ('queued','running')");return null;}); }
    @Scheduled(fixedDelay=30000)
    public void expire() {
        try {
            var jobs=memory.jdbc.query("SELECT id,account_id FROM assistant_memory_extraction WHERE status IN ('queued','running') AND created_at<TIMESTAMPADD(SECOND,-180,CURRENT_TIMESTAMP)",(r,n)->Map.entry(r.getString("id"),r.getInt("account_id")));
            jobs.forEach(j->fail(j.getValue(),j.getKey(),"Extraction timed out. Please retry."));
        } catch(RuntimeException ignored) { /* Retry database recovery later. */ }
    }
}
