package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.AssistantEngine;
import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.*;
import com.example.ipd_sp_back_end.repository.AssistantConversationRepository;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AssistantConversationTests {
    JdbcTemplate jdbc;
    AssistantConversationService service;
    AssistantConversationRepository repository;
    TransactionTemplate transactions;
    List<Runnable> jobs;
    AtomicInteger calls;
    @BeforeEach void setup() throws Exception {
        var data = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(data);
        jdbc.execute("CREATE TABLE auth_user(id INT PRIMARY KEY)");
        jdbc.update("INSERT INTO auth_user VALUES(1),(2)");
        String schema = new String(new ClassPathResource("assistant-schema.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
        for (String statement : schema.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
        repository = new AssistantConversationRepository(jdbc);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(data));
        calls = new AtomicInteger(); jobs = new ArrayList<>();
        AssistantEngine engine = (request, mode) -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            calls.incrementAndGet(); return new AssistantEngine.AssistantAnswer(request.getMessage() + ":" + request.getContext().get("age"), "api");
        };
        service = new AssistantConversationService(repository, transactions, engine, jobs::add);
    }
    SendRequest sendRequest(long revision, String request, String question, String edit) {
        return new SendRequest(request, revision, question, "zh-CN", Map.of("age", 20), List.of(), "api", edit);
    }
    @Test void ownerIsolationRevisionAndModelRunsOutsideTransaction() {
        var conversation = service.create(1);
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.history(2, conversation.id(), null, 80)).getStatusCode().value());
        String request = UUID.randomUUID().toString();
        var task = service.send(1, conversation.id(), sendRequest(0, request, "健康", null));
        assertEquals(1, service.history(1, conversation.id(), null, 80).messages().size());
        assertEquals(0, calls.get());
        assertEquals(task.id(), service.send(1, conversation.id(), sendRequest(0, request, "健康", null)).id());
        assertEquals(1, jobs.size());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.send(1, conversation.id(), sendRequest(1, UUID.randomUUID().toString(), "饮食", null))).getStatusCode().value());
        jobs.remove(0).run();
        var history = service.history(1, conversation.id(), null, 80);
        assertEquals(2, history.messages().size()); assertEquals("健康:20", history.messages().get(1).content());
        assertEquals("completed", service.task(1, conversation.id(), request).status());
        assertNull(jdbc.queryForObject("SELECT payload_json FROM assistant_generation_task WHERE id=?", String.class, task.id()));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.rename(1, conversation.id(), new RenameRequest(0L, "old"))).getStatusCode().value());
    }
    @Test void failedSaveCannotStartModelAndRestartReleasesPendingTask() {
        String id = service.create(1).id();
        jdbc.execute("ALTER TABLE assistant_generation_task ADD CONSTRAINT reject_tasks CHECK (status <> 'queued')");
        assertThrows(RuntimeException.class, () -> service.send(1, id, sendRequest(0, UUID.randomUUID().toString(), "健康", null)));
        assertTrue(jobs.isEmpty()); assertTrue(service.history(1, id, null, 80).messages().isEmpty());
        jdbc.execute("ALTER TABLE assistant_generation_task DROP CONSTRAINT reject_tasks");
        String request = UUID.randomUUID().toString();
        service.send(1, id, sendRequest(0, request, "健康", null));
        service.recoverAfterRestart();
        assertEquals("failed", service.task(1, id, request).status());
        jobs.remove(0).run(); assertEquals(0, calls.get()); assertNull(service.history(1, id, null, 80).activeTask());
    }
    @Test void completeHistoryIsPagedAndImportIsIdempotentAndAccountScoped() {
        var imported = new ArrayList<ImportedMessage>();
        for (int i=0; i<105; i++) imported.add(new ImportedMessage(i%2==0 ? "user" : "assistant", "message " + i, null));
        var request = new ImportRequest(imported, "en");
        var conversation = service.importHistory(1, request);
        assertEquals(conversation.id(), service.importHistory(1, request).id());
        assertNotEquals(conversation.id(), service.importHistory(2, request).id());
        var recent = service.history(1, conversation.id(), null, 80);
        assertEquals(80, recent.messages().size()); assertEquals("message 25", recent.messages().get(0).content()); assertNotNull(recent.nextBefore());
        var earlier = service.history(1, conversation.id(), recent.nextBefore(), 80);
        assertEquals(25, earlier.messages().size()); assertNull(earlier.nextBefore()); assertNull(earlier.messages().get(0).createdAt());
    }
    @Test void editTruncatesAndDeletedRequestCannotResurrectHistory() {
        String id = service.create(1).id();
        String request = UUID.randomUUID().toString();
        service.send(1, id, sendRequest(0, request, "first", null)); jobs.remove(0).run();
        var first = service.history(1, id, null, 80);
        service.send(1, id, sendRequest(first.conversation().revision(), UUID.randomUUID().toString(), "second", null)); jobs.remove(0).run();
        var latest = service.history(1, id, null, 80);
        service.send(1, id, sendRequest(latest.conversation().revision(), UUID.randomUUID().toString(), "edited", first.messages().get(0).id()));
        jobs.remove(0).run();
        var edited = service.history(1, id, null, 80);
        assertEquals(2, edited.messages().size()); assertEquals("edited", edited.messages().get(0).content());
        assertEquals(410, assertThrows(ResponseStatusException.class, () -> service.send(1, id, sendRequest(0, request, "first", null))).getStatusCode().value());
        service.deleteMessages(1, id, new DeleteMessagesRequest(edited.conversation().revision(), List.of(edited.messages().get(0).id(), edited.messages().get(1).id())));
        assertTrue(service.history(1, id, null, 80).messages().isEmpty());
    }
    @Test void deletionDuringGenerationDoesNotRecreateConversation() {
        String id = service.create(1).id();
        service = new AssistantConversationService(repository, transactions, (request, mode) -> {
            long revision = service.history(1, id, null, 80).conversation().revision();
            service.delete(1, id, revision);
            return new AssistantEngine.AssistantAnswer("late", "api");
        }, jobs::add);
        service.send(1, id, sendRequest(0, UUID.randomUUID().toString(), "health", null)); jobs.remove(0).run();
        assertTrue(service.list(1, 0, 20).items().isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM assistant_message", Integer.class));
    }
    @Test void answerSaveFailureAndExecutorRejectionStayVisibleWithoutAutomaticRegeneration() {
        String id = service.create(1).id();
        String request = UUID.randomUUID().toString();
        jdbc.execute("ALTER TABLE assistant_message ADD CONSTRAINT reject_answer CHECK (role <> 'assistant')");
        service.send(1, id, sendRequest(0, request, "health", null)); jobs.remove(0).run();
        assertEquals(1, calls.get()); assertEquals("failed", service.task(1, id, request).status());
        assertEquals(1, service.history(1, id, null, 80).messages().size());
        service.send(1, id, sendRequest(0, request, "health", null)); assertTrue(jobs.isEmpty());
        var busy = new AssistantConversationService(repository, transactions, (r,m) -> { fail("No generation expected"); return null; }, job -> { throw new RejectedExecutionException(); });
        String other = busy.create(2).id();
        String rejected = UUID.randomUUID().toString();
        busy.send(2, other, sendRequest(0, rejected, "health", null));
        assertEquals("failed", busy.task(2, other, rejected).status());
    }
    @Test void timeoutRejectsLateResultAndReleasesConversation() {
        String id = service.create(1).id();
        service = new AssistantConversationService(repository, transactions, (request, mode) -> {
            jdbc.update("UPDATE assistant_generation_task SET started_at=? WHERE conversation_id=?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(181)), id);
            service.expireStalledTasks();
            return new AssistantEngine.AssistantAnswer("late", "api");
        }, jobs::add);
        String request = UUID.randomUUID().toString();
        service.send(1, id, sendRequest(0, request, "health", null)); jobs.remove(0).run();
        assertEquals("failed", service.task(1, id, request).status());
        assertNull(service.history(1, id, null, 80).activeTask());
        assertEquals(1, service.history(1, id, null, 80).messages().size());
    }
    @Test void competingDevicesOnlyAcceptOneRequestAndContextsDoNotMix() throws Exception {
        String id = service.create(1).id();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var results = new ArrayList<Future<Boolean>>();
            Executor noGeneration = ignored -> { };
            var concurrent = new AssistantConversationService(repository, transactions, (r,m) -> new AssistantEngine.AssistantAnswer("unused", "api"), noGeneration);
            for (int i=0; i<2; i++) results.add(pool.submit(() -> {
                start.await();
                try { concurrent.send(1, id, sendRequest(0, UUID.randomUUID().toString(), "health", null)); return true; }
                catch (ResponseStatusException exception) { assertEquals(409, exception.getStatusCode().value()); return false; }
            }));
            start.countDown();
            int accepted = 0; for (var result : results) if (result.get(5, TimeUnit.SECONDS)) accepted++;
            assertEquals(1, accepted);
        } finally { pool.shutdownNow(); }
        String other = service.create(2).id();
        service.send(2, other, new SendRequest(UUID.randomUUID().toString(), 0L, "health", "en", Map.of("age", 60), List.of(), "api", null)); jobs.remove(0).run();
        assertEquals("health:60", service.history(2, other, null, 80).messages().get(1).content());
    }
}
