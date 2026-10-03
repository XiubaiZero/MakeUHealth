package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.preferences.AssistantPreferencesService;
import com.example.ipd_sp_back_end.assistant.memory.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AssistantPreferencesTests {
    JdbcTemplate jdbc;TransactionTemplate tx;AssistantPreferencesService service;
    void schema(String name) throws Exception {
        String sql=new String(new ClassPathResource(name).getInputStream().readAllBytes(),StandardCharsets.UTF_8).replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
        for(String part:sql.split(";"))if(!part.isBlank())jdbc.execute(part);
    }
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE auth_user(id INT PRIMARY KEY)");jdbc.update("INSERT INTO auth_user VALUES(1),(2)");schema("assistant-preferences-schema.sql");service=new AssistantPreferencesService(jdbc,tx);
    }
    @Test void defaultPersistsAcrossInstancesAndAccountsAreIsolated() {
        assertTrue(service.get(1).enterSendEnabled());assertEquals(0,service.get(1).revision());service.update(1,false,0);
        assertFalse(new AssistantPreferencesService(jdbc,tx).get(1).enterSendEnabled());assertTrue(service.get(2).enterSendEnabled());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.update(1,true,0)).getStatusCode().value());assertFalse(service.get(1).enterSendEnabled());
        assertThrows(IllegalArgumentException.class,()->service.update(1,true,-1));
    }
    @Test void reinitializationDoesNotResetSavedValueAndDeleteAccountCascades() throws Exception {
        service.update(1,false,0);schema("assistant-preferences-schema.sql");assertFalse(service.get(1).enterSendEnabled());
        jdbc.update("DELETE FROM auth_user WHERE id=1");assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM assistant_preferences WHERE account_id=1",Integer.class));
    }
    @Test void preferenceChangesAndMemoryClearsAreIndependent() throws Exception {
        schema("assistant-schema.sql");schema("assistant-memory-schema.sql");var memory=new AssistantMemoryService(jdbc,tx,new AssistantMemoryProperties());var personal=new AssistantPersonalMemoryService(memory);
        long revision=personal.state(1).settings().revision();service.update(1,false,0);assertEquals(revision,personal.state(1).settings().revision());
        personal.clear(1,revision);assertFalse(service.get(1).enterSendEnabled());assertEquals(1,service.get(1).revision());
    }
    @Test void concurrentStaleWritesHaveOneWinner() throws Exception {
        service.get(1);var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        Callable<Boolean> write=()->{gate.await();try{service.update(1,false,0);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}};
        try{var a=pool.submit(write);var b=pool.submit(write);gate.countDown();assertNotEquals(a.get(),b.get());assertEquals(1,service.get(1).revision());}finally{pool.shutdownNow();}
    }
}
