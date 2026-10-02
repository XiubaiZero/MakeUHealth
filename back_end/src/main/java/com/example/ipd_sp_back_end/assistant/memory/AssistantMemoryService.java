package com.example.ipd_sp_back_end.assistant.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.Message;

@Service
public class AssistantMemoryService {
    protected final JdbcTemplate jdbc;
    protected final TransactionTemplate transactions;
    protected final AssistantMemoryProperties properties;
    private final ObjectMapper json=new ObjectMapper();
    public AssistantMemoryService(JdbcTemplate jdbc, TransactionTemplate transactions, AssistantMemoryProperties properties) {
        this.jdbc=jdbc; this.transactions=transactions; this.properties=properties;
    }
    public void lock(int account) {
        jdbc.queryForObject("SELECT id FROM auth_user WHERE id=? FOR UPDATE",Integer.class,account);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_account WHERE account_id=?",Integer.class,account)==0)
            jdbc.update("INSERT INTO assistant_memory_account(account_id) VALUES(?)",account);
    }
    public void ensure(int account, String conversation) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_conversation WHERE account_id=? AND id=?",Integer.class,account,conversation)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_conversation WHERE conversation_id=?",Integer.class,conversation)==0)
            jdbc.update("INSERT INTO assistant_memory_conversation(conversation_id) VALUES(?)",conversation);
    }
    public record Settings(boolean enabled,long revision,long contentRevision,boolean hasSummary,long coveredSequence) { }
    public Settings settings(int account,String conversation) {
        return transactions.execute(s -> {
            lock(account); ensure(account,conversation);
            return jdbc.queryForObject("SELECT * FROM assistant_memory_conversation WHERE conversation_id=?",
                (r,n)->new Settings(r.getBoolean("enabled"),r.getLong("revision"),r.getLong("content_revision"),r.getString("summary")!=null,r.getLong("covered_sequence")),conversation);
        });
    }
    public Settings toggle(int account,String conversation,long expected,boolean enabled) {
        return transactions.execute(s -> {
            lock(account); ensure(account,conversation);
            if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_conversation WHERE conversation_id=? AND revision=?",Integer.class,conversation,expected)!=1)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Memory changed on another device. Refresh and try again.");
            jdbc.update("UPDATE assistant_memory_conversation SET enabled=?,revision=revision+1 WHERE conversation_id=?",enabled,conversation);
            return settings(account,conversation);
        });
    }
    protected static final String ROUND_SQL = "SELECT q.sequence_no qs,a.sequence_no ans,q.content question,a.content answer FROM assistant_message q JOIN assistant_message a ON a.conversation_id=q.conversation_id AND a.role='assistant' WHERE q.conversation_id=? AND q.role='user' AND q.sequence_no<? AND (EXISTS(SELECT 1 FROM assistant_generation_task t WHERE t.conversation_id=q.conversation_id AND t.question_id=q.id AND t.answer_id=a.id AND t.status='completed' AND a.source='api') OR (q.source='imported' AND a.source='imported' AND a.sequence_no=q.sequence_no+1))";
    protected List<MemorySnapshot.Round> rounds(String conversation,long before,String extra,String order,int limit,Object... extras) {
        var args=new ArrayList<Object>(); args.add(conversation); args.add(before); args.addAll(Arrays.asList(extras)); args.add(limit);
        return jdbc.query(ROUND_SQL+extra+" ORDER BY q.sequence_no "+order+" LIMIT ?",(r,n)->new MemorySnapshot.Round(r.getLong("qs"),r.getLong("ans"),r.getString("question"),r.getString("answer")),args.toArray());
    }
    public MemorySnapshot snapshot(int account,String conversation,String questionId) {
        ensure(account,conversation);
        var setting=settings(account,conversation);
        long accountRevision=jdbc.queryForObject("SELECT revision FROM assistant_memory_account WHERE account_id=?",Long.class,account);
        if (!setting.enabled()) return new MemorySnapshot(false,accountRevision,setting.revision(),setting.contentRevision(),"",0,List.of(),List.of(),0,List.of());
        long before=jdbc.queryForObject("SELECT sequence_no FROM assistant_message WHERE conversation_id=? AND id=?",Long.class,conversation,questionId);
        String summary=jdbc.queryForObject("SELECT summary FROM assistant_memory_conversation WHERE conversation_id=?",String.class,conversation);
        long covered=setting.coveredSequence();
        var recent=new ArrayList<>(rounds(conversation,before," AND a.sequence_no>?","DESC",properties.getRecentRounds(),covered)); Collections.reverse(recent);
        long oldestRecent=recent.isEmpty()?before:recent.get(0).questionSequence();
        var older=rounds(conversation,before," AND a.sequence_no>? AND a.sequence_no<?","ASC",20,covered,oldestRecent);
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM ("+ROUND_SQL+") memory_rounds",Integer.class,conversation,before);
        return new MemorySnapshot(true,accountRevision,setting.revision(),setting.contentRevision(),summary==null?"":summary,covered,recent,older,count,List.of());
    }
    public boolean valid(int account,String conversation,MemorySnapshot snapshot) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_conversation c JOIN assistant_memory_account a ON a.account_id=? WHERE c.conversation_id=? AND c.revision=? AND c.content_revision=? AND a.revision=?",Integer.class,account,conversation,snapshot.settingsRevision(),snapshot.contentRevision(),snapshot.accountRevision())!=1) return false;
        return true;
    }
    public void invalidate(int account,String conversation) {
        ensure(account,conversation);
        jdbc.update("UPDATE assistant_memory_conversation SET summary=NULL,covered_sequence=0,content_revision=content_revision+1 WHERE conversation_id=?",conversation);
    }
    public void saveMetadata(String message,Map<String,Object> metadata) {
        if (metadata==null || metadata.isEmpty()) return;
        try { jdbc.update("INSERT INTO assistant_memory_message(message_id,metadata_json) VALUES(?,?)",message,json.writeValueAsString(metadata)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }
    public void saveSummary(String conversation,MemorySnapshot snapshot,String summary,long through) {
        if (summary==null) return;
        jdbc.update("UPDATE assistant_memory_conversation SET summary=?,covered_sequence=? WHERE conversation_id=? AND content_revision=? AND revision=? AND covered_sequence<=?",summary,through,conversation,snapshot.contentRevision(),snapshot.settingsRevision(),through);
    }
    public List<Message> enrich(List<Message> messages) {
        if (messages.isEmpty()) return messages;
        var details=new HashMap<String,Map<String,Object>>();
        jdbc.query("SELECT * FROM assistant_memory_message WHERE message_id IN ("+String.join(",",Collections.nCopies(messages.size(),"?"))+")",r -> {
            try { details.put(r.getString("message_id"),json.readValue(r.getString("metadata_json"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {})); }
            catch (Exception exception) { throw new IllegalStateException("Invalid memory metadata.",exception); }
        },messages.stream().map(Message::id).toArray());
        return messages.stream().map(m->new Message(m.id(),m.sequence(),m.role(),m.content(),m.language(),m.source(),m.suggestionPrompts(),m.createdAt(),details.get(m.id()))).toList();
    }
}
