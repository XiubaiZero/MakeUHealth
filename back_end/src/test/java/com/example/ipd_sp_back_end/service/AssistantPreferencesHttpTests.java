package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.preferences.AssistantPreferencesService;
import com.example.ipd_sp_back_end.controller.AssistantPreferencesController;
import com.example.ipd_sp_back_end.security.*;
import com.example.ipd_sp_back_end.config.SecurityConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Real authenticated HTTP endpoints with an independent disposable H2 database. */
@SpringBootTest(classes=AssistantPreferencesHttpTests.Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:preferences-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.sql.init.mode=never","assistant.deepseek.api-key="})
class AssistantPreferencesHttpTests {
    @Configuration @EnableAutoConfiguration
    @Import({AssistantPreferencesController.class,AssistantPreferencesService.class,CurrentAccount.class,JwtService.class,JwtAuthenticationFilter.class,SecurityConfig.class})
    static class Application { }
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;@Autowired JwtService jwt;
    final ObjectMapper json=new ObjectMapper();final HttpClient client=HttpClient.newHttpClient();
    @BeforeEach void setup(){
        jdbc.execute("CREATE TABLE IF NOT EXISTS auth_user(id INT PRIMARY KEY)");jdbc.update("MERGE INTO auth_user KEY(id) VALUES(1),(2)");
        new ResourceDatabasePopulator(new ClassPathResource("assistant-preferences-schema.sql")).execute(jdbc.getDataSource());jdbc.update("DELETE FROM assistant_preferences");
    }
    HttpResponse<String> request(String method,String token,Object body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/assistant/preferences")).header("Content-Type","application/json");
        if(token!=null)builder.header("Authorization","Bearer "+token);
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void authenticationIsolationRevisionAndValidationUseRealHttp() throws Exception {
        assertEquals(401,request("GET",null,null).statusCode());assertEquals(401,request("GET","invalid",null).statusCode());
        String one=jwt.generateToken(1,"one@example.test"),two=jwt.generateToken(2,"two@example.test");
        assertTrue(json.readTree(request("GET",one,null).body()).path("enterSendEnabled").asBoolean());
        var updated=request("PATCH",one,Map.of("enterSendEnabled",false,"expectedRevision",0));assertEquals(200,updated.statusCode());assertEquals(1,json.readTree(updated.body()).path("revision").asInt());
        assertFalse(json.readTree(request("GET",one,null).body()).path("enterSendEnabled").asBoolean());assertTrue(json.readTree(request("GET",two,null).body()).path("enterSendEnabled").asBoolean());
        assertEquals(409,request("PATCH",one,Map.of("enterSendEnabled",true,"expectedRevision",0)).statusCode());
        assertEquals(400,request("PATCH",one,Map.of("enterSendEnabled",true)).statusCode());
        assertEquals(400,request("PATCH",one,Map.of("enterSendEnabled",true,"expectedRevision",-1)).statusCode());
    }
    @Test void unavailableStorageKeeps503ErrorBody() throws Exception {
        jdbc.execute("DROP TABLE assistant_preferences");var response=request("GET",jwt.generateToken(1,"one@example.test"),null);
        assertEquals(503,response.statusCode());assertEquals("Chat preferences are unavailable. Please retry.",json.readTree(response.body()).path("message").asText());
    }
}
