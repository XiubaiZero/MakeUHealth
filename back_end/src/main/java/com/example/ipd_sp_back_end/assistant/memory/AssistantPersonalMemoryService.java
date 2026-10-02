package com.example.ipd_sp_back_end.assistant.memory;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Supplier;

@Service
public class AssistantPersonalMemoryService {
    private final AssistantMemoryService memory;
    public AssistantPersonalMemoryService(AssistantMemoryService memory) { this.memory=memory; }
    public record AccountSettings(boolean enabled,long revision,int capacity,int pendingCapacity) { }
    public record Proof(String messageId,String evidence) { }
    public record Item(String id,String category,String content,String status,long revision,String sourceConversationId,List<Proof> sources) { }
    public record State(AccountSettings settings,List<Item> items) { }
    public record Change(Long expectedRevision,String category,String content,Boolean confirmed,String replaceId,Long replaceRevision) { }
    public <T> T transaction(int account,Supplier<T> action) { return memory.transactions.execute(s->{memory.lock(account);return action.get();}); }
    private ResponseStatusException conflict() { return new ResponseStatusException(HttpStatus.CONFLICT,"Memory changed on another device. Refresh and try again."); }
    public static void validate(String category,String content) {
        if (!Set.of("diet","training","routine","other").contains(Objects.toString(category,"")) || content==null || content.isBlank() || content.codePointCount(0,content.length())>200) throw new IllegalArgumentException("Memory must contain 1 to 200 characters and a valid category.");
    }
    static String normalized(String content) { return AssistantMemoryExtractor.hash(Normalizer.normalize(content,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim()); }
    public AccountSettings settings(int account) {
        return memory.jdbc.queryForObject("SELECT * FROM assistant_memory_account WHERE account_id=?",(r,n)->new AccountSettings(r.getBoolean("enabled"),r.getLong("revision"),memory.properties.getCapacity(),50),account);
    }
    private void revision(int account,Long expected) { if(expected==null || expected!=settings(account).revision()) throw conflict(); }
    public void bump(int account) { memory.jdbc.update("UPDATE assistant_memory_account SET revision=revision+1 WHERE account_id=?",account); }
    public State state(int account) { return transaction(account,()->new State(settings(account),items(account))); }
    public AccountSettings toggle(int account,long expected,boolean enabled) {
        return transaction(account,()->{ revision(account,expected);memory.jdbc.update("UPDATE assistant_memory_account SET enabled=?,revision=revision+1 WHERE account_id=?",enabled,account);return settings(account); });
    }
    public List<Item> items(int account) {
        var proofs=new HashMap<String,List<Proof>>();
        memory.jdbc.query("SELECT s.* FROM assistant_memory_source s JOIN assistant_memory_item i ON i.id=s.memory_id WHERE i.account_id=? ORDER BY s.message_id",r->{proofs.computeIfAbsent(r.getString("memory_id"),k->new ArrayList<>()).add(new Proof(r.getString("message_id"),r.getString("evidence")));},account);
        return memory.jdbc.query("SELECT * FROM assistant_memory_item WHERE account_id=? ORDER BY updated_at DESC,id",(r,n)->new Item(r.getString("id"),r.getString("category"),r.getString("content"),r.getString("status"),r.getLong("revision"),r.getString("source_conversation_id"),List.copyOf(proofs.getOrDefault(r.getString("id"),List.of()))),account);
    }
    private Item owned(int account,String id,Long expected) {
        var item=items(account).stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Memory not found."));
        if(expected==null || expected!=item.revision()) throw conflict(); return item;
    }
    private void quota(int account,String status,int delta) {
        int count=memory.jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_item WHERE account_id=? AND status=?",Integer.class,account,status);
        int capacity="confirmed".equals(status)?memory.properties.getCapacity():50;
        if(count+delta>capacity) throw new ResponseStatusException(HttpStatus.CONFLICT,"Memory capacity reached. Delete or replace an entry first.");
    }
    public Item add(int account,Change request) {
        validate(request.category(),request.content());
        return transaction(account,()->{revision(account,request.expectedRevision());quota(account,"confirmed",1);String id=UUID.randomUUID().toString();
            memory.jdbc.update("INSERT INTO assistant_memory_item(id,account_id,category,content,status,normalized_hash) VALUES(?,?,?,?,'confirmed',?)",id,account,request.category(),request.content().trim(),normalized(request.content()));bump(account);return owned(account,id,0L);});
    }
    public Item change(int account,String id,Change request) {
        validate(request.category(),request.content());
        return transaction(account,()->{
            var item=owned(account,id,request.expectedRevision());boolean confirm="confirmed".equals(item.status()) || Boolean.TRUE.equals(request.confirmed());
            if (request.replaceId()!=null) {
                if (id.equals(request.replaceId()) || !confirm) throw new IllegalArgumentException("Choose a different confirmed memory to replace.");
                var replace=owned(account,request.replaceId(),request.replaceRevision());
                if (!"confirmed".equals(replace.status())) throw new IllegalArgumentException("Only a confirmed memory can be replaced.");
                memory.jdbc.update("DELETE FROM assistant_memory_item WHERE id=? AND account_id=?",replace.id(),account);
            }
            if(confirm && !"confirmed".equals(item.status())) quota(account,"confirmed",1);
            memory.jdbc.update("UPDATE assistant_memory_item SET category=?,content=?,status=?,normalized_hash=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND account_id=?",request.category(),request.content().trim(),confirm?"confirmed":"pending",normalized(request.content()),id,account);
            bump(account);return owned(account,id,item.revision()+1);
        });
    }
    public void delete(int account,String id,long expected) { transaction(account,()->{owned(account,id,expected);memory.jdbc.update("DELETE FROM assistant_memory_item WHERE id=? AND account_id=?",id,account);bump(account);return null;}); }
    public void clear(int account,long expected) { transaction(account,()->{revision(account,expected);memory.jdbc.update("DELETE FROM assistant_memory_item WHERE account_id=?",account);bump(account);return null;}); }
    public List<String> saveCandidates(int account,String conversation,List<AssistantMemoryExtractor.Source> sources,AssistantMemoryExtractor.Result result) {
        var hashes=new HashMap<String,String>();sources.forEach(s->hashes.put(s.messageId(),s.hash()));var ids=new ArrayList<String>();
        for (var candidate:result.candidates()) {
            String hash=normalized(candidate.content());
            if(memory.jdbc.queryForObject("SELECT COUNT(*) FROM assistant_memory_item WHERE account_id=? AND normalized_hash=?",Integer.class,account,hash)>0) continue;
            quota(account,"pending",1);String id=UUID.randomUUID().toString();
            memory.jdbc.update("INSERT INTO assistant_memory_item(id,account_id,category,content,status,normalized_hash,source_conversation_id) VALUES(?,?,?,?,'pending',?,?)",id,account,candidate.category(),candidate.content(),hash,conversation);
            for(var proof:candidate.sources()) memory.jdbc.update("INSERT INTO assistant_memory_source(memory_id,message_id,content_hash,evidence) VALUES(?,?,?,?)",id,proof.messageId(),hashes.get(proof.messageId()),proof.evidence());ids.add(id);
        }
        return List.copyOf(ids);
    }
}
