package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.AssistantEngine;
import com.example.ipd_sp_back_end.assistant.AssistantGenerationRequest;
import com.example.ipd_sp_back_end.assistant.memory.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.*;
import com.example.ipd_sp_back_end.repository.AssistantConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class AssistantConversationService {
    private static final Logger log = LoggerFactory.getLogger(AssistantConversationService.class);
    private final AssistantConversationRepository repository;
    private final TransactionTemplate transactions;
    private final AssistantEngine engine;
    private final Executor executor;
    private final AssistantMemoryService memory;
    private final ObjectMapper json = new ObjectMapper();
    private static final List<String> SUGGESTIONS = List.of(
            "Based on my latest health data, what should I focus on this week?",
            "Create a 7-day workout and meal plan for my fat-loss goal.",
            "Compare my muscle-gain and fat-loss progress and suggest priorities.",
            "How should I adjust my daily diet based on my weight and goal?",
            "Give me a practical weekly routine for training, sleep, and recovery.");

    public AssistantConversationService(AssistantConversationRepository repository, TransactionTemplate transactions,
            AssistantEngine engine, @Qualifier("assistantTaskExecutor") Executor executor) {
        this(repository,transactions,engine,executor,null);
    }
    @Autowired
    public AssistantConversationService(AssistantConversationRepository repository, TransactionTemplate transactions,
            AssistantEngine engine, @Qualifier("assistantTaskExecutor") Executor executor, AssistantMemoryService memory) {
        this.repository = repository; this.transactions = transactions; this.engine = engine; this.executor = executor;
        this.memory = memory;
    }
    private <T> T transaction(Supplier<T> action) { return transactions.execute(status -> action.get()); }
    private <T> T transaction(int account,Supplier<T> action) { return transaction(() -> { if (memory!=null) memory.lock(account); return action.get(); }); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    private Conversation owned(int account, String id) {
        Conversation conversation = repository.owned(account, id, true);
        if (conversation == null) throw error(HttpStatus.NOT_FOUND, "Conversation not found.");
        return conversation;
    }
    private void revision(Conversation conversation, Long expected) {
        if (expected == null || expected < 0) throw error(HttpStatus.BAD_REQUEST, "A conversation revision is required.");
        if (expected != conversation.revision()) throw error(HttpStatus.CONFLICT, "Conversation changed on another device. Refresh and try again.");
    }
    private void idle(String id) {
        if (repository.active(id) != null) throw error(HttpStatus.CONFLICT, "This conversation is generating a reply.");
    }
    private String language(String value) { return "zh-CN".equals(value) ? "zh-CN" : "en"; }
    private String uuid() { return UUID.randomUUID().toString(); }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception exception) { throw error(HttpStatus.BAD_REQUEST, "Invalid assistant data."); }
    }
    public ConversationPage list(int account, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) throw error(HttpStatus.BAD_REQUEST, "Invalid pagination.");
        var rows = repository.list(account, offset, limit + 1);
        return new ConversationPage(rows.stream().limit(limit).toList(), rows.size() > limit);
    }
    public Conversation create(int account) {
        return transaction(account, () -> { String id = uuid(); repository.create(account, id, null, null); return owned(account, id); });
    }
    public History history(int account, String id, Long before, int limit) {
        if ((before != null && before < 1) || limit < 1 || limit > 80) throw error(HttpStatus.BAD_REQUEST, "Invalid pagination.");
        return transaction(account, () -> {
            Conversation conversation = owned(account, id);
            var descending = repository.messages(id, before, limit + 1);
            var rows = new ArrayList<>(descending.stream().limit(limit).toList());
            Collections.reverse(rows);
            return new History(conversation, memory==null?rows:memory.enrich(rows), descending.size() > limit ? rows.get(0).sequence() : null, repository.active(id), repository.failed(id));
        });
    }
    public Conversation rename(int account, String id, RenameRequest request) {
        if (request.title() == null || request.title().trim().isEmpty() || request.title().trim().codePointCount(0, request.title().trim().length()) > 100) throw error(HttpStatus.BAD_REQUEST, "Title must contain 1 to 100 characters.");
        return transaction(account, () -> { var conversation = owned(account, id); revision(conversation, request.expectedRevision()); repository.rename(id, request.title().trim()); return owned(account, id); });
    }
    public void delete(int account, String id, Long expected) {
        transaction(account, () -> { revision(owned(account, id), expected); if (memory!=null) memory.invalidate(account,id); repository.delete(id); return null; });
    }
    public void deleteMessages(int account, String id, DeleteMessagesRequest request) {
        if (request.messageIds() == null || request.messageIds().isEmpty() || request.messageIds().size() > 1000) throw error(HttpStatus.BAD_REQUEST, "Select messages to delete.");
        transaction(account, () -> {
            revision(owned(account, id), request.expectedRevision()); idle(id);
            if (memory!=null) memory.invalidateSources(account,id,request.messageIds());
            for (String message : new HashSet<>(request.messageIds())) {
                if (repository.message(id, message) == null) throw error(HttpStatus.NOT_FOUND, "Message not found.");
                repository.deleteMessage(id, message);
            }
            repository.invalidateDeletedTasks(id); repository.touch(id); return null;
        });
    }
    public Task task(int account, String id, String requestId) {
        return transaction(account, () -> { owned(account, id); var task = repository.byRequest(id, requestId); if (task == null) throw error(HttpStatus.NOT_FOUND, "Generation task not found."); return task; });
    }
    public Task send(int account, String id, SendRequest request) {
        try { UUID.fromString(request.requestId()); }
        catch (Exception exception) { throw error(HttpStatus.BAD_REQUEST, "A valid request ID is required."); }
        String question = request.message() == null ? "" : request.message().trim();
        if (question.isEmpty() || question.length() > 20000) throw error(HttpStatus.BAD_REQUEST, "Message must contain 1 to 20000 characters.");
        var payload = new SendRequest(request.requestId(), request.expectedRevision(), question, language(request.language()),
                request.context(), request.constraints(), "local".equals(request.mode()) ? "local" : "api", request.editMessageId());
        if (!"local".equals(payload.mode())) {
            var input=new AssistantChatRequest(); input.setMessage(question); input.setLanguage(payload.language()); input.setContext(payload.context()); input.setConstraints(payload.constraints());
            try { engine.validate(input); } catch (IllegalArgumentException exception) { throw error(HttpStatus.BAD_REQUEST,exception.getMessage()); }
        }
        String raw = encode(payload);
        if (raw.getBytes(StandardCharsets.UTF_8).length > 262144) throw error(HttpStatus.BAD_REQUEST, "Assistant context is too large.");
        boolean[] created = {false};
        Task task = transaction(account, () -> {
            Conversation conversation = owned(account, id);
            // An accepted request is replayed before the revision check.
            Task previous = repository.byRequest(id, request.requestId());
            if (previous != null) {
                if ("deleted".equals(previous.status())) throw error(HttpStatus.GONE, "This request belongs to deleted history.");
                return previous;
            }
            revision(conversation, request.expectedRevision()); idle(id);
            String questionId;
            if (request.editMessageId() != null) {
                Message original = repository.message(id, request.editMessageId());
                if (original == null || !"user".equals(original.role())) throw error(HttpStatus.NOT_FOUND, "Original question not found.");
                questionId = original.id();
                if (memory!=null) memory.invalidateFrom(account,id,original.sequence());
                repository.edit(id, original, question, payload.language());
            } else {
                questionId = uuid();
                repository.append(id, questionId, "user", question, payload.language(), "user", null, false);
            }
            repository.defaultTitle(id, question.substring(0, question.offsetByCodePoints(0, Math.min(30, question.codePointCount(0, question.length())))));
            if (memory!=null && !"local".equals(payload.mode())) {
                var input=new AssistantChatRequest();input.setMessage(question);input.setLanguage(payload.language());input.setContext(payload.context());input.setConstraints(payload.constraints());
                try { engine.validate(new AssistantGenerationRequest(input,payload.mode(),memory.snapshot(account,id,questionId))); }
                catch(IllegalArgumentException exception) { throw error(HttpStatus.BAD_REQUEST,exception.getMessage()); }
            }
            repository.task(uuid(), id, request.requestId(), questionId, raw);
            repository.touch(id); created[0] = true;
            return repository.byRequest(id, request.requestId());
        });
        if (created[0]) {
            try { executor.execute(() -> generate(account, id, task)); }
            catch (RuntimeException exception) { fail(account, id, task.id(), "Generation is busy. Please retry."); }
        }
        return task;
    }
    private void generate(int account, String conversation, Task task) {
        try {
            record Claimed(String raw,MemorySnapshot snapshot) { }
            Claimed claimed=transaction(account, () -> {
                owned(account,conversation); String raw=repository.claim(task.id());
                return raw==null?null:new Claimed(raw,memory==null?MemorySnapshot.empty():memory.snapshot(account,conversation,task.questionId()));
            });
            if (claimed==null) return;
            String raw=claimed.raw();
            SendRequest request = json.readValue(raw, SendRequest.class);
            var chat = new AssistantChatRequest();
            chat.setMessage(request.message()); chat.setLanguage(request.language()); chat.setContext(request.context()); chat.setConstraints(request.constraints());
            // The model call deliberately runs after the claim transaction has committed.
            var answer = engine.generate(new AssistantGenerationRequest(chat,request.mode(),claimed.snapshot()));
            if (answer.answer() == null || answer.answer().isBlank()) throw new IllegalStateException("Empty answer.");
            transaction(account, () -> {
                if (repository.owned(account, conversation, true) == null) return null;
                Task current = repository.byRequest(conversation, task.requestId());
                if (current == null || !"running".equals(current.status()) || repository.message(conversation, task.questionId()) == null) return null;
                if (memory!=null && !memory.valid(account,conversation,claimed.snapshot())) {
                    repository.fail(task.id(),"Memory changed while generating. Please retry."); repository.touch(conversation); return null;
                }
                String answerId = uuid();
                boolean suggestions = answer.answer().toLowerCase(Locale.ROOT).contains("try asking:") || answer.answer().contains("可以试着问");
                repository.append(conversation, answerId, "assistant", answer.answer(), request.language(), answer.source(), suggestions ? SUGGESTIONS : null, false);
                if (memory!=null) memory.saveMetadata(answerId,answer.memory());
                if (memory!=null) memory.saveSummary(conversation,claimed.snapshot(),answer.summaryUpdate(),answer.summaryThrough());
                repository.finish(task.id(), answerId); repository.touch(conversation); return null;
            });
        } catch (Exception exception) {
            fail(account, conversation, task.id(), "Generation interrupted. Please retry.");
        }
    }
    private void fail(int account, String conversation, String task, String message) {
        try {
            transaction(account, () -> { if (repository.owned(account, conversation, true) != null) { repository.fail(task, message); repository.touch(conversation); } return null; });
        } catch (RuntimeException exception) { log.warn("Unable to persist assistant task status; timeout recovery will retry."); }
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recoverAfterRestart() { transaction(() -> { repository.interruptPending(); return null; }); }
    @Scheduled(fixedDelay = 30000)
    public void expireStalledTasks() {
        try {
            for (String conversation : repository.timedOutConversations()) {
                Integer account = repository.account(conversation);
                if (account == null) continue;
                transaction(account, () -> {
                    if (repository.owned(account, conversation, true) == null) return null;
                    Task task = repository.active(conversation);
                    if (task != null && repository.expire(task.id())) repository.touch(conversation);
                    return null;
                });
            }
        } catch (RuntimeException exception) { log.warn("Assistant timeout recovery is waiting for database availability."); }
    }
    public Conversation importHistory(int account, ImportRequest request) {
        if (request.messages() == null || request.messages().isEmpty() || request.messages().size() > 1000) throw error(HttpStatus.BAD_REQUEST, "Invalid imported history.");
        for (ImportedMessage message : request.messages()) {
            if (message == null || !("user".equals(message.role()) || "assistant".equals(message.role())) || message.content() == null || message.content().trim().isEmpty() || message.content().length() > 1000000
                    || (message.suggestionPrompts() != null && (message.suggestionPrompts().size() > 6 || message.suggestionPrompts().stream().anyMatch(p -> p == null || p.length() > 1000)))) throw error(HttpStatus.BAD_REQUEST, "Invalid imported message.");
        }
        String raw = encode(request.messages());
        if (raw.getBytes(StandardCharsets.UTF_8).length > 5242880) throw error(HttpStatus.BAD_REQUEST, "Imported history is too large.");
        String fingerprint;
        try { fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
        return transaction(account, () -> {
            repository.lockAccount(account);
            var existing = repository.imported(account, fingerprint);
            if (existing != null) return existing;
            String id = uuid(); repository.create(account, id, "zh-CN".equals(request.language()) ? "导入的历史记录" : "Imported history", fingerprint);
            for (ImportedMessage message : request.messages()) repository.append(id, uuid(), message.role(), message.content(), null, "imported", message.suggestionPrompts(), true);
            repository.touch(id); return owned(account, id);
        });
    }
}
