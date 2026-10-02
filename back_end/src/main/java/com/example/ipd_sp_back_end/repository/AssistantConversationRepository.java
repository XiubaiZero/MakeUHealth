package com.example.ipd_sp_back_end.repository;

import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** SQL only. The service owns transaction and account boundaries. */
@Repository
public class AssistantConversationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    public AssistantConversationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private String time(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant().toString();
    }
    private final RowMapper<Conversation> conversationMapper = (r, n) -> new Conversation(r.getString("id"),
            r.getString("title"), r.getLong("revision"), time(r, "created_at"), time(r, "updated_at"));
    private final RowMapper<Message> messageMapper = (r, n) -> new Message(r.getString("id"), r.getLong("sequence_no"),
            r.getString("role"), r.getString("content"), r.getString("language"), r.getString("source"),
            suggestions(r.getString("suggestions_json")), time(r, "created_at"));
    private final RowMapper<Task> taskMapper = (r, n) -> new Task(r.getString("id"), r.getString("request_id"),
            r.getString("question_id"), r.getString("answer_id"), r.getString("status"), r.getString("error_message"));
    private List<String> suggestions(String raw) {
        try { return raw == null ? null : json.readValue(raw, new TypeReference<List<String>>() { }); }
        catch (Exception exception) { throw new IllegalStateException("Invalid saved suggestion data.", exception); }
    }
    public List<Conversation> list(int account, int offset, int limit) {
        return jdbc.query("SELECT * FROM assistant_conversation WHERE account_id=? ORDER BY updated_at DESC,id DESC LIMIT ? OFFSET ?", conversationMapper, account, limit, offset);
    }
    public Conversation owned(int account, String id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM assistant_conversation WHERE account_id=? AND id=?" + (lock ? " FOR UPDATE" : ""), conversationMapper, account, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public void lockAccount(int account) {
        jdbc.queryForObject("SELECT id FROM auth_user WHERE id=? FOR UPDATE", Integer.class, account);
    }
    public Conversation imported(int account, String fingerprint) {
        var rows = jdbc.query("SELECT * FROM assistant_conversation WHERE account_id=? AND import_fingerprint=?", conversationMapper, account, fingerprint);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public void create(int account, String id, String title, String fingerprint) {
        var now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO assistant_conversation(id,account_id,title,import_fingerprint,created_at,updated_at) VALUES(?,?,?,?,?,?)", id, account, title, fingerprint, now, now);
    }
    public void touch(String id) {
        jdbc.update("UPDATE assistant_conversation SET revision=revision+1,updated_at=? WHERE id=?", Timestamp.from(Instant.now()), id);
    }
    public void rename(String id, String title) { jdbc.update("UPDATE assistant_conversation SET title=? WHERE id=?", title, id); touch(id); }
    public void defaultTitle(String id, String title) { jdbc.update("UPDATE assistant_conversation SET title=? WHERE id=? AND title IS NULL", title, id); }
    public void delete(String id) { jdbc.update("DELETE FROM assistant_conversation WHERE id=?", id); }
    public List<Message> messages(String id, Long before, int limit) {
        return before == null
                ? jdbc.query("SELECT * FROM assistant_message WHERE conversation_id=? ORDER BY sequence_no DESC LIMIT ?", messageMapper, id, limit)
                : jdbc.query("SELECT * FROM assistant_message WHERE conversation_id=? AND sequence_no<? ORDER BY sequence_no DESC LIMIT ?", messageMapper, id, before, limit);
    }
    public Message message(String conversation, String id) {
        var rows = jdbc.query("SELECT * FROM assistant_message WHERE conversation_id=? AND id=?", messageMapper, conversation, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public void append(String conversation, String id, String role, String content, String language, String source, List<String> suggestions, boolean imported) {
        Long sequence = jdbc.queryForObject("SELECT next_sequence FROM assistant_conversation WHERE id=?", Long.class, conversation);
        String raw = null;
        try { if (suggestions != null) raw = json.writeValueAsString(suggestions); }
        catch (Exception exception) { throw new IllegalArgumentException("Invalid suggestions.", exception); }
        jdbc.update("INSERT INTO assistant_message(id,conversation_id,sequence_no,role,content,language,source,suggestions_json,created_at) VALUES(?,?,?,?,?,?,?,?,?)", id, conversation, sequence, role, content, language, source, raw, imported ? null : Timestamp.from(Instant.now()));
        jdbc.update("UPDATE assistant_conversation SET next_sequence=next_sequence+1 WHERE id=?", conversation);
    }
    public void edit(String conversation, Message message, String content, String language) {
        jdbc.update("UPDATE assistant_generation_task SET status='deleted',payload_json=NULL WHERE conversation_id=? AND question_id=?", conversation, message.id());
        jdbc.update("DELETE FROM assistant_message WHERE conversation_id=? AND sequence_no>?", conversation, message.sequence());
        jdbc.update("UPDATE assistant_message SET content=?,language=? WHERE conversation_id=? AND id=?", content, language, conversation, message.id());
        invalidateDeletedTasks(conversation);
    }
    public void deleteMessage(String conversation, String id) { jdbc.update("DELETE FROM assistant_message WHERE conversation_id=? AND id=?", conversation, id); }
    public void invalidateDeletedTasks(String conversation) {
        // Retain request tombstones so a retry cannot resurrect edited/deleted messages.
        jdbc.update("UPDATE assistant_generation_task SET status='deleted',payload_json=NULL WHERE conversation_id=? AND (NOT EXISTS (SELECT 1 FROM assistant_message m WHERE m.id=question_id) OR (answer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM assistant_message m WHERE m.id=answer_id)))", conversation);
    }
    public Task byRequest(String conversation, String request) {
        var rows = jdbc.query("SELECT * FROM assistant_generation_task WHERE conversation_id=? AND request_id=?", taskMapper, conversation, request);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public Task active(String conversation) {
        var rows = jdbc.query("SELECT * FROM assistant_generation_task WHERE conversation_id=? AND status IN ('queued','running') LIMIT 1", taskMapper, conversation);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public Task failed(String conversation) {
        var rows = jdbc.query("SELECT * FROM assistant_generation_task WHERE conversation_id=? AND status='failed' AND EXISTS (SELECT 1 FROM assistant_message m WHERE m.id=question_id) ORDER BY created_at DESC LIMIT 1", taskMapper, conversation);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public void task(String id, String conversation, String request, String question, String payload) {
        jdbc.update("INSERT INTO assistant_generation_task(id,conversation_id,request_id,question_id,status,payload_json,created_at) VALUES(?,?,?,?,'queued',?,?)", id, conversation, request, question, payload, Timestamp.from(Instant.now()));
    }
    public String claim(String id) {
        if (jdbc.update("UPDATE assistant_generation_task SET status='running',started_at=? WHERE id=? AND status='queued'", Timestamp.from(Instant.now()), id) != 1) return null;
        return jdbc.queryForObject("SELECT payload_json FROM assistant_generation_task WHERE id=?", String.class, id);
    }
    public void finish(String id, String answer) { jdbc.update("UPDATE assistant_generation_task SET status='completed',answer_id=?,payload_json=NULL,error_message=NULL WHERE id=?", answer, id); }
    public void fail(String id, String error) { jdbc.update("UPDATE assistant_generation_task SET status='failed',error_message=?,payload_json=NULL WHERE id=? AND status IN ('queued','running')", error, id); }
    public boolean expire(String id) {
        return jdbc.update("UPDATE assistant_generation_task SET status='failed',error_message='Generation timed out. Please retry.',payload_json=NULL WHERE id=? AND status='running' AND started_at<?", id, Timestamp.from(Instant.now().minusSeconds(180))) == 1;
    }
    public void interruptPending() {
        jdbc.update("UPDATE assistant_conversation SET revision=revision+1 WHERE EXISTS (SELECT 1 FROM assistant_generation_task t WHERE t.conversation_id=assistant_conversation.id AND t.status IN ('queued','running'))");
        jdbc.update("UPDATE assistant_generation_task SET status='failed',error_message='Generation interrupted. Please retry.',payload_json=NULL WHERE status IN ('queued','running')");
    }
    public List<String> timedOutConversations() {
        return jdbc.queryForList("SELECT DISTINCT conversation_id FROM assistant_generation_task WHERE status='running' AND started_at<?", String.class, Timestamp.from(Instant.now().minusSeconds(180)));
    }
    public Integer account(String id) {
        var rows = jdbc.queryForList("SELECT account_id FROM assistant_conversation WHERE id=?", Integer.class, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
