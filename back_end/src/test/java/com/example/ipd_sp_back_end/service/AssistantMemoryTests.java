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
    AssistantPersonalMemoryService personal() { return new AssistantPersonalMemoryService(memory); }
    AssistantPersonalMemoryService.Change change(long revision,String content,boolean confirm) { return new AssistantPersonalMemoryService.Change(revision,"diet",content,confirm,null,null); }
    AssistantMemoryExtractor.Result candidate(String message,String content) {
        return new AssistantMemoryExtractor.Result(List.of(new AssistantMemoryExtractor.Candidate("diet",content,List.of(new AssistantMemoryExtractor.Evidence(message,content)))),new ChatCompletionResult("{}","fake","stop",1,Map.of()));
    }
    @Test void onlyConfirmedOwnedFactsEnterNewConversationsAndGlobalSwitchPersists() {
        var personal=personal(); personal.state(1);var manual=personal.add(1,change(0,"不吃海鲜",true));
        String a=conversations.create(1).id();turn(a,"饮食计划");assertEquals(List.of("不吃海鲜"),generations.get(0).memory().facts());
        String b=conversations.create(2).id();assertEquals(404,assertThrows(ResponseStatusException.class,()->personal.delete(2,manual.id(),0)).getStatusCode().value());
        assertTrue(personal.state(2).items().isEmpty());assertNotEquals(a,b);
        long revision=personal.state(1).settings().revision();personal.toggle(1,revision,false);turn(a,"饮食调整");assertTrue(generations.get(1).memory().facts().isEmpty());assertEquals(1,generations.get(1).memory().rounds().size());
        assertEquals(1,personal.state(1).items().size());assertEquals(409,assertThrows(ResponseStatusException.class,()->personal.toggle(1,revision,true)).getStatusCode().value());
    }
    @Test void candidatesRequireConfirmationAndSourceDeletionRemovesOnlyRelatedEntries() {
        var personal=personal();String id=conversations.create(1).id();turn(id,"不吃海鲜");turn(id,"偏好豆类");
        var history=conversations.history(1,id,null,80);var first=history.messages().get(0);var last=history.messages().get(2);
        personal.transaction(1,()->{ personal.saveCandidates(1,id,List.of(new AssistantMemoryExtractor.Source(first.id(),first.content(),AssistantMemoryExtractor.hash(first.content()))),candidate(first.id(),first.content()));personal.saveCandidates(1,id,List.of(new AssistantMemoryExtractor.Source(last.id(),last.content(),AssistantMemoryExtractor.hash(last.content()))),candidate(last.id(),last.content()));return null; });
        assertTrue(memory.facts(1).isEmpty());var items=personal.state(1).items();var confirmed=personal.change(1,items.get(0).id(),change(0,items.get(0).content(),true));assertEquals(1,memory.facts(1).size());
        var manual=personal.add(1,change(personal.state(1).settings().revision(),"每周训练三次",true));
        conversations.deleteMessages(1,id,new DeleteMessagesRequest(history.conversation().revision(),List.of(last.id())));
        assertTrue(personal.state(1).items().stream().noneMatch(i->i.sources().stream().anyMatch(s->s.messageId().equals(last.id()))));
        assertTrue(personal.state(1).items().stream().anyMatch(i->i.sources().stream().anyMatch(s->s.messageId().equals(first.id()))));
        var fresh=conversations.history(1,id,null,80);conversations.delete(1,id,fresh.conversation().revision());assertEquals(List.of(manual.id()),personal.state(1).items().stream().map(AssistantPersonalMemoryService.Item::id).toList());
    }
    @Test void capacityRequiresExplicitReplacementAndStaleMemoryEditsAreRejected() {
        settings.setCapacity(1);var personal=personal();personal.state(1);var first=personal.add(1,change(0,"偏好素食",true));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->personal.add(1,change(1,"不吃海鲜",true))).getStatusCode().value());
        var updated=personal.change(1,first.id(),change(0,"偏好豆类",true));assertEquals(1,updated.revision());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->personal.delete(1,first.id(),0)).getStatusCode().value());
        String conversation=conversations.create(1).id();turn(conversation,"不吃海鲜");var source=conversations.history(1,conversation,null,80).messages().get(0);
        var ids=personal.transaction(1,()->personal.saveCandidates(1,conversation,List.of(new AssistantMemoryExtractor.Source(source.id(),source.content(),AssistantMemoryExtractor.hash(source.content()))),candidate(source.id(),source.content())));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->personal.change(1,ids.get(0),change(0,"不吃海鲜",true))).getStatusCode().value());
        var replacement=personal.change(1,ids.get(0),new AssistantPersonalMemoryService.Change(0L,"diet","不吃海鲜",true,first.id(),1L));
        assertEquals(List.of(replacement.id()),personal.state(1).items().stream().map(AssistantPersonalMemoryService.Item::id).toList());
        personal.clear(1,personal.state(1).settings().revision());assertTrue(personal.state(1).items().isEmpty());
        assertThrows(IllegalArgumentException.class,()->personal.add(1,change(0,"😀".repeat(201),true)));
    }
    @Test void explicitExtractionRunsOutsideTransactionIsIdempotentAndCannotResurrectAfterClear() {
        String id=conversations.create(1).id();turn(id,"我不吃海鲜，偏好豆类。");var personal=personal();var captured=new ArrayList<AssistantPrompt>();
        ChatModelClient client=new ChatModelClient() {
            public boolean isConfigured(){return true;}
            public String complete(AssistantPrompt prompt){throw new AssertionError();}
            public ChatCompletionResult complete(AssistantPrompt prompt,ChatCompletionOptions options) {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());captured.add(prompt);assertTrue(options.json());
                var source=conversations.history(1,id,null,80).messages().get(0);
                if(captured.size()==2) personal.clear(1,personal.state(1).settings().revision());
                return new ChatCompletionResult("{\"memories\":[{\"category\":\"diet\",\"content\":\"不吃海鲜\",\"sources\":[{\"messageId\":\""+source.id()+"\",\"evidence\":\"不吃海鲜\"}]}]}","fake","stop",1,Map.of());
            }
        };
        var extractor=new AssistantMemoryExtractor(client,settings);var queue=new ArrayList<Runnable>();var service=new AssistantMemoryExtractionService(memory,personal,extractor,queue::add);
        var request=new AssistantMemoryExtractionService.Request(UUID.randomUUID().toString(),conversations.history(1,id,null,80).conversation().revision(),"zh-CN");
        service.create(1,id,request);assertEquals(1,queue.size());service.create(1,id,request);assertEquals(1,queue.size());queue.remove(0).run();
        assertEquals("completed",service.get(1,id,request.requestId()).status());assertEquals(1,personal.state(1).items().size());assertTrue(memory.facts(1).isEmpty());
        assertFalse(captured.get(0).userPrompt().contains("answer:"));
        var next=new AssistantMemoryExtractionService.Request(UUID.randomUUID().toString(),request.expectedRevision(),"zh-CN");service.create(1,id,next);queue.remove(0).run();assertEquals("failed",service.get(1,id,next.requestId()).status());assertTrue(personal.state(1).items().isEmpty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(2,id,request.requestId())).getStatusCode().value());
    }
    @Test void extractorRejectsUnsupportedEvidenceAndAssistantOnlyFacts() {
        ChatModelClient client=new ChatModelClient(){public boolean isConfigured(){return true;}public String complete(AssistantPrompt p){return "{\"memories\":[{\"category\":\"diet\",\"content\":\"不吃海鲜\",\"sources\":[{\"messageId\":\"assistant-id\",\"evidence\":\"不吃海鲜\"}]}]}";}};
        var extractor=new AssistantMemoryExtractor(client,settings);
        assertThrows(IllegalStateException.class,()->extractor.extract(List.of(new AssistantMemoryExtractor.Source("user-id","如何训练？","hash")),"zh-CN"));
    }
    @Test void editingLaterSourcePreservesEarlierMemoryAndRejectsLateAnswerAfterAccountChange() {
        String id=conversations.create(1).id();turn(id,"偏好燕麦");turn(id,"不吃海鲜");var personal=personal();var before=conversations.history(1,id,null,80);var earlier=before.messages().get(0);var later=before.messages().get(2);
        personal.transaction(1,()->{personal.saveCandidates(1,id,List.of(new AssistantMemoryExtractor.Source(earlier.id(),earlier.content(),AssistantMemoryExtractor.hash(earlier.content()))),candidate(earlier.id(),earlier.content()));return null;});
        var item=personal.state(1).items().get(0);personal.change(1,item.id(),change(0,item.content(),true));
        conversations.send(1,id,new SendRequest(UUID.randomUUID().toString(),before.conversation().revision(),"偏好豆类","zh-CN",Map.of(),List.of(),"api",later.id()));jobs.remove(0).run();
        assertEquals(1,personal.state(1).items().size());assertEquals(earlier.id(),personal.state(1).items().get(0).sources().get(0).messageId());
        var snapshot=generations.get(2).memory();assertTrue(memory.valid(1,id,snapshot));personal.clear(1,personal.state(1).settings().revision());assertFalse(memory.valid(1,id,snapshot));
    }
    @Test void mandatoryMemoryOverBudgetRejectsBeforeSavingQuestionOrCallingModel() {
        settings.setInputBudget(1024);var personal=personal();personal.state(1);
        for(int index=0;index<10;index++) personal.add(1,change(personal.state(1).settings().revision(),"健康习惯"+index+"x".repeat(190),true));
        ChatModelClient client=new ChatModelClient(){public boolean isConfigured(){return true;}public String complete(AssistantPrompt p){throw new AssertionError("Must reject before calling model");}};
        var classifier=new RuleBasedAssistantIntentClassifier();var props=new DeepSeekProperties();
        var assistant=new AssistantService(classifier,new AssistantReplyCatalog(),new AssistantContextFormatter(),new AssistantPromptBuilder(),client,new AssistantResponseProcessor(),new AssistantContextBudget(settings),props,new AssistantSummaryProcessor(client,settings));
        var service=new AssistantConversationService(new AssistantConversationRepository(jdbc),tx,new JavaAssistantEngine(assistant,classifier),jobs::add,memory);
        String id=service.create(1).id();
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.send(1,id,new SendRequest(UUID.randomUUID().toString(),0L,"给我健康训练建议","zh-CN",Map.of(),List.of(),"api",null))).getStatusCode().value());
        assertTrue(service.history(1,id,null,80).messages().isEmpty());assertTrue(jobs.isEmpty());
    }
    @Test void accountChangeDuringGenerationDiscardsLateAnswerAndKeepsRetryableQuestion() {
        var personal=personal();String id=conversations.create(1).id();
        var engine=new AssistantEngine(){
            public AssistantAnswer generate(AssistantChatRequest request,String mode){throw new AssertionError();}
            public AssistantAnswer generate(AssistantGenerationRequest request){assertFalse(TransactionSynchronizationManager.isActualTransactionActive());personal.clear(1,personal.state(1).settings().revision());return new AssistantAnswer("迟到回答","api");}
        };
        var service=new AssistantConversationService(new AssistantConversationRepository(jdbc),tx,engine,jobs::add,memory);
        var task=service.send(1,id,new SendRequest(UUID.randomUUID().toString(),0L,"健康训练建议","zh-CN",Map.of(),List.of(),"api",null));jobs.remove(0).run();
        assertEquals("failed",service.task(1,id,task.requestId()).status());assertEquals(1,service.history(1,id,null,80).messages().size());
    }
}
