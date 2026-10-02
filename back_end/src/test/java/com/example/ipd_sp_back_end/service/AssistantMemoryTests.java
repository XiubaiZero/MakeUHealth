package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import com.example.ipd_sp_back_end.assistant.memory.*;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.*;
import com.example.ipd_sp_back_end.repository.AssistantConversationRepository;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssistantMemoryTests {
    JdbcTemplate jdbc; TransactionTemplate tx; AssistantMemoryService memory; AssistantConversationService conversations;
    AssistantMemoryProperties settings; List<Runnable> jobs; List<AssistantGenerationRequest> generations;
    @BeforeEach void setup() throws Exception {
        var data=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(data); tx=new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE auth_user(id INT PRIMARY KEY)"); jdbc.update("INSERT INTO auth_user VALUES(1),(2)");
        for (String file:List.of("assistant-schema.sql","assistant-memory-schema.sql")) {
            String schema=new String(new ClassPathResource(file).getInputStream().readAllBytes(),StandardCharsets.UTF_8).replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
            for (String sql:schema.split(";")) if (!sql.isBlank()) jdbc.execute(sql);
        }
        settings=new AssistantMemoryProperties(); memory=new AssistantMemoryService(jdbc,tx,settings); jobs=new ArrayList<>(); generations=new ArrayList<>();
        var engine=new AssistantEngine() {
            public AssistantAnswer generate(AssistantChatRequest request,String mode) { throw new AssertionError("Must use generation boundary."); }
            public AssistantAnswer generate(AssistantGenerationRequest request) {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); generations.add(request);
                return new AssistantAnswer("answer: "+request.request().getMessage(),"api",Map.of("historyMessages",request.memory().history().size()),null,0);
            }
        };
        conversations=new AssistantConversationService(new AssistantConversationRepository(jdbc),tx,engine,jobs::add,memory);
    }
    void turn(String id,String question) {
        long revision=conversations.history(1,id,null,80).conversation().revision();
        conversations.send(1,id,new SendRequest(UUID.randomUUID().toString(),revision,question,"zh-CN",Map.of(),List.of(),"api",null)); jobs.remove(0).run();
    }
    @Test void historyIsServerOwnedOrderedAndCurrentQuestionAppearsOnlyOnce() {
        String id=conversations.create(1).id(); turn(id,"健康训练计划"); turn(id,"展开第二条");
        var request=generations.get(1); assertEquals(2,request.memory().history().size());
        assertEquals("健康训练计划",request.memory().history().get(0).content()); assertEquals("assistant",request.memory().history().get(1).role());
        assertEquals("展开第二条",request.request().getMessage());
        assertEquals(2,conversations.history(1,id,null,80).messages().get(3).memory().get("historyMessages"));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->memory.settings(2,id)).getStatusCode().value());
    }
    @Test void togglePersistsAndDoesNotDeleteHistoryAndRejectsStaleWrites() {
        String id=conversations.create(1).id(); turn(id,"健康计划");
        var original=memory.settings(1,id); assertTrue(original.enabled()); memory.toggle(1,id,original.revision(),false);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->memory.toggle(1,id,original.revision(),true)).getStatusCode().value());
        turn(id,"训练计划"); assertFalse(generations.get(1).memory().enabled()); assertEquals(4,conversations.history(1,id,null,80).messages().size());
    }
    @Test void editedHistoryInvalidatesSnapshotAndImportedPairsRemainOrdered() {
        var imported=List.of(new ImportedMessage("user","不吃海鲜",null),new ImportedMessage("assistant","使用豆类",null),new ImportedMessage("user","没有回答的问题",null));
        String id=conversations.importHistory(1,new ImportRequest(imported,"zh-CN")).id(); turn(id,"饮食建议");
        assertEquals(1,generations.get(0).memory().rounds().size());
        var snapshot=generations.get(0).memory();
        tx.execute(s->{memory.lock(1); memory.invalidate(1,id); return null;}); assertFalse(memory.valid(1,id,snapshot));
    }
    @Test void budgetDropsWholeOldRoundsAndNeverTruncatesCurrentQuestion() {
        settings.setInputBudget(1024); var budget=new AssistantContextBudget(settings);
        var prompt=new AssistantPrompt("Health","Current question",List.of(new AssistantPrompt.Message("user","x".repeat(700)),new AssistantPrompt.Message("assistant","y".repeat(700)),new AssistantPrompt.Message("user","recent"),new AssistantPrompt.Message("assistant","answer")));
        var fitted=budget.fit(prompt,4096); assertEquals(2,fitted.history().size()); assertEquals("Current question",fitted.userPrompt());
        assertThrows(IllegalArgumentException.class,()->budget.fit(new AssistantPrompt("Health","x".repeat(1100)),4096));
    }
    @Test void summaryCoverageIsStoredAndSourceEditsPreventStaleSummaryWrite() {
        String id=conversations.create(1).id(); turn(id,"训练计划"); turn(id,"饮食计划");
        var snapshot=generations.get(1).memory();
        tx.execute(s->{memory.lock(1);memory.saveSummary(id,snapshot,"明确个人事实",2);return null;});
        assertTrue(memory.settings(1,id).hasSummary());
        tx.execute(s->{memory.lock(1);memory.invalidate(1,id);memory.saveSummary(id,snapshot,"迟到摘要",2);return null;});
        assertFalse(memory.settings(1,id).hasSummary());
    }
    @Test void contextualFollowUpUsesRealMessageRolesAndLegacyScopeStaysUnchanged() {
        var captured=new ArrayList<AssistantPrompt>();
        ChatModelClient client=new ChatModelClient() {
            public boolean isConfigured(){return true;}
            public String complete(AssistantPrompt prompt){captured.add(prompt);return "使用豆类。";}
        };
        var service=AssistantServiceTestSupport.service(client); var request=new AssistantChatRequest(); request.setMessage("展开第二条"); request.setLanguage("zh-CN");
        assertTrue(service.ask(request).contains("只能回答")); assertTrue(captured.isEmpty());
        var snapshot=new MemorySnapshot(true,0,0,0,"",0,List.of(new MemorySnapshot.Round(1,2,"饮食计划","1. 豆类。2. 蔬菜。")),List.of(),1,List.of());
        assertEquals("使用豆类。",service.askWithMemory(request,snapshot).answer()); assertEquals(2,captured.get(0).history().size());
    }
}
