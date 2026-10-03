package com.example.ipd_sp_back_end;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

// Explicit opt-in prevents tests from modifying the developer's normal IPD database.
@EnabledIfEnvironmentVariable(named = "HMS_TEST_DATABASE_URL", matches = "jdbc:mysql:.*")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"assistant.deepseek.api-key=", "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
class IpdSpBackEndApplicationTests {
    @Value("${local.server.port}") int port;
    @Autowired DataSource dataSource;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("HMS_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("HMS_TEST_DATABASE_USER", "root"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("HMS_TEST_DATABASE_PASSWORD", ""));
    }

    private HttpResponse<String> request(String method, String path, String token, Object body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode ok(String method, String path, String token, Object body) throws Exception {
        var response = request(method, path, token, body);
        assertEquals(200, response.statusCode(), method + " " + path + ": " + response.body());
        return response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
    }

    private String newAccount() throws Exception {
        var credentials = Map.of("account", UUID.randomUUID() + "@example.test", "password", "test-password-123");
        ok("POST", "/auth/register", null, credentials);
        String token = ok("POST", "/auth/login", null, credentials).path("token").asText();
        assertFalse(token.isBlank());
        ok("POST", "/users", token, Map.of("age", 30, "gender", "male", "height", 175, "weight", 75));
        return token;
    }

    @Test void assistantPreferencesAreAuthenticatedPersistedAndAccountIsolated() throws Exception {
        assertEquals(401,request("GET","/assistant/preferences",null,null).statusCode());
        String one=newAccount(),two=newAccount();
        assertTrue(ok("GET","/assistant/preferences",one,null).path("enterSendEnabled").asBoolean());
        var changed=ok("PATCH","/assistant/preferences",one,Map.of("enterSendEnabled",false,"expectedRevision",0));
        assertEquals(1,changed.path("revision").asLong());
        assertFalse(ok("GET","/assistant/preferences",one,null).path("enterSendEnabled").asBoolean());
        assertTrue(ok("GET","/assistant/preferences",two,null).path("enterSendEnabled").asBoolean());
        assertEquals(409,request("PATCH","/assistant/preferences",one,Map.of("enterSendEnabled",true,"expectedRevision",0)).statusCode());
        assertEquals(400,request("PATCH","/assistant/preferences",one,Map.of("enterSendEnabled",true)).statusCode());
    }

    @Test
    void cloudChatPersistsAcrossClientsAndRejectsOtherAccounts() throws Exception {
        String owner = newAccount(), other = newAccount();
        var conversation = ok("POST", "/assistant/conversations", owner, null);
        String id = conversation.path("id").asText();
        var preflight = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/assistant/conversations/" + id))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "PATCH")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preflight.statusCode());
        assertTrue(preflight.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("PATCH"));
        assertEquals(404, request("GET", "/assistant/conversations/" + id + "/messages", other, null).statusCode());
        String requestId = UUID.randomUUID().toString();
        var body = Map.of("requestId", requestId, "expectedRevision", 0, "message", "你能做什么", "language", "zh-CN", "context", Map.of(), "mode", "api");
        var sent = request("POST", "/assistant/conversations/" + id + "/turns", owner, body);
        assertEquals(202, sent.statusCode());
        JsonNode task = null;
        for (int attempt=0; attempt<100; attempt++) {
            task = ok("GET", "/assistant/conversations/" + id + "/turns/" + requestId, owner, null);
            if ("completed".equals(task.path("status").asText())) break;
            Thread.sleep(20);
        }
        assertEquals("completed", task.path("status").asText());
        var history = ok("GET", "/assistant/conversations/" + id + "/messages", owner, null);
        assertEquals(2, history.path("messages").size());
        assertTrue(history.path("messages").get(1).path("content").asText().contains("健康数据"));
        assertEquals(200, request("POST", "/assistant/conversations/" + id + "/turns", owner, body).statusCode());
        assertEquals(409, request("PATCH", "/assistant/conversations/" + id, owner, Map.of("expectedRevision", 0, "title", "stale")).statusCode());
        assertEquals(404, request("DELETE", "/assistant/conversations/" + id + "?expectedRevision=" + history.path("conversation").path("revision").asLong(), other, null).statusCode());
        ok("DELETE", "/assistant/conversations/" + id + "?expectedRevision=" + history.path("conversation").path("revision").asLong(), owner, null);
        assertEquals(404, request("GET", "/assistant/conversations/" + id + "/messages", owner, null).statusCode());
    }

    @Test
    void memoryManagementPersistsWithOwnershipRevisionsAndDisabledModelExtraction() throws Exception {
        String owner=newAccount(),other=newAccount();
        assertEquals(401,request("GET","/assistant/memory",null,null).statusCode());
        var settings=ok("GET","/assistant/memory",owner,null).path("settings");
        var item=ok("POST","/assistant/memory/items",owner,Map.of("expectedRevision",settings.path("revision").asLong(),"category","diet","content","我长期偏好素食。"));
        String id=item.path("id").asText();assertEquals("confirmed",item.path("status").asText());
        assertEquals(0,ok("GET","/assistant/memory",other,null).path("items").size());
        assertEquals(404,request("DELETE","/assistant/memory/items/"+id+"?expectedRevision=0",other,null).statusCode());
        ok("PATCH","/assistant/memory/items/"+id,owner,Map.of("expectedRevision",0,"category","diet","content","我偏好燕麦。"));
        assertEquals(409,request("PATCH","/assistant/memory/items/"+id,owner,Map.of("expectedRevision",0,"category","diet","content","旧编辑")).statusCode());
        long revision=ok("GET","/assistant/memory",owner,null).path("settings").path("revision").asLong();
        ok("PATCH","/assistant/memory",owner,Map.of("expectedRevision",revision,"enabled",false));
        assertFalse(ok("GET","/assistant/memory",owner,null).path("settings").path("enabled").asBoolean());
        ok("PATCH","/assistant/memory",owner,Map.of("expectedRevision",revision+1,"enabled",true));
        var conversation=ok("POST","/assistant/conversations",owner,null);
        var extract=request("POST","/assistant/conversations/"+conversation.path("id").asText()+"/memory/extractions",owner,Map.of("requestId",UUID.randomUUID().toString(),"expectedRevision",0,"language","zh-CN"));
        assertEquals(503,extract.statusCode());assertTrue(json.readTree(extract.body()).path("message").asText().contains("not configured"));
        long current=ok("GET","/assistant/memory",owner,null).path("settings").path("revision").asLong();
        assertEquals(204,request("DELETE","/assistant/memory?expectedRevision="+current,owner,null).statusCode());
        assertEquals(0,ok("GET","/assistant/memory",owner,null).path("items").size());
        assertEquals(1,ok("GET","/assistant/conversations",owner,null).path("items").size());
    }

    @Test
    void authenticationAndHealthRecordsStayWithinTheCurrentAccount() throws Exception {
        assertEquals(401, request("GET", "/users", null, null).statusCode());
        String owner = newAccount(), other = newAccount();
        int ownerId = ok("GET", "/users", owner, null).get(0).path("id").asInt();
        var record = ok("POST", "/health-records/user/" + ownerId, owner,
                Map.of("systolic", 120, "diastolic", 80, "fbg", 5.2, "heartRate", 70,
                        "oxyhemoglobin", 98, "recordedAt", LocalDateTime.now().toString()));
        assertEquals(75, record.path("weightSnapshot").asInt());
        ok("PUT", "/users/" + ownerId, owner, Map.of("age", 30, "gender", "male", "height", 175, "weight", 74));
        assertEquals(75, ok("GET", "/health-records/" + record.path("id").asInt(), owner, null).path("weightSnapshot").asInt());
        assertEquals(0, ok("GET", "/health-records/user/" + ownerId, other, null).size());
        assertEquals(404, request("GET", "/health-records/" + record.path("id").asInt(), other, null).statusCode());
    }

    @Test
    void foodTargetsAndGoalProgressRoundTrip() throws Exception {
        String token = newAccount();
        int userId = ok("GET", "/users", token, null).get(0).path("id").asInt();
        assertTrue(ok("GET", "/food-library", token, null).size() >= 6);
        assertEquals(2.6, ok("GET", "/food-library/search?name=Rice", token, null).path("protein").asDouble(), 0.001);
        ok("POST", "/food-intake", token, Map.of("foodName", "Rice", "amount", 100, "unit", "g",
                "calories", 116, "nutrients", "Carbohydrate", "mealType", "lunch", "intakeTime", LocalDateTime.now().toString()));
        assertEquals(116, ok("GET", "/food-intake/stats/" + userId + "/day", token, null).get(0).path("calories").asInt());
        for (int target : new int[]{600, 650}) {
            ok("POST", "/meal-targets", token, Map.of("targetDate", LocalDate.now().toString(), "lunchTarget", target));
        }
        assertEquals(650, ok("GET", "/meal-targets?date=" + LocalDate.now(), token, null).path("lunchTarget").asInt());
        var goal = ok("POST", "/fitness-goals", token, Map.of("goalType", "weight_loss",
                "currentValue", 75, "targetValue", 70, "targetDate", LocalDate.now().plusWeeks(10).toString()));
        String path = "/fitness-goals/goal/" + goal.path("id").asInt() + "/progress";
        ok("POST", path, token, Map.of("currentValue", 74));
        ok("PUT", path, token, Map.of("currentValue", 73.5));
        var progress = ok("GET", path, token, null);
        assertEquals(1, progress.size());
        assertEquals(73.5, progress.get(0).path("currentValue").asDouble(), 0.001);
    }

    @Test
    void remindersAreAccountScopedAndAssistantReportsMissingConfiguration() throws Exception {
        String owner = newAccount(), other = newAccount();
        var reminder = Map.of("reminderType", "exercise", "reminderTime", LocalDateTime.now().plusMinutes(5).toString(),
                "repeatPattern", "monthly", "enabled", true, "note", "Test reminder");
        var saved = ok("POST", "/reminders", owner, reminder);
        String path = "/reminders/" + saved.path("id").asLong();
        assertEquals(0, ok("GET", "/reminders", other, null).size());
        assertEquals(404, request("PUT", path, other, reminder).statusCode());
        assertEquals(404, request("DELETE", path, other, null).statusCode());
        ok("PUT", path, owner, Map.of("reminderType", "exercise", "reminderTime", saved.path("reminderTime").asText(),
                "repeatPattern", "monthly", "enabled", false));
        assertFalse(ok("GET", "/reminders", owner, null).get(0).path("enabled").asBoolean());
        ok("DELETE", path, owner, null);
        assertEquals(0, ok("GET", "/reminders", owner, null).size());
        assertTrue(ok("POST", "/assistant/chat", owner, Map.of("message", "Help with my fitness plan"))
                .path("answer").asText().contains("API key is missing"));
    }

    @Test
    void legacySchemaUpgradesAndInitializationCanRepeatWithoutDataLoss() throws Exception {
        String database = "hms_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection()) {
            String original = connection.getCatalog();
            connection.createStatement().execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4");
            try {
                connection.setCatalog(database);
                var isolated = new SingleConnectionDataSource(connection, true);
                // The legacy snapshot's DROP statements run only in this new, disposable database.
                new ResourceDatabasePopulator(new FileSystemResource("../integrated_ipd.sql")).execute(isolated);
                var jdbc = new JdbcTemplate(isolated);
                int users = jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class);
                jdbc.update("UPDATE food_library SET calories = 777 WHERE food_name = 'Rice'");
                var initializer = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"), new ClassPathResource("assistant-schema.sql"), new ClassPathResource("data.sql"));
                initializer.execute(isolated);
                jdbc.update("UPDATE food_library SET protein = 9 WHERE food_name = 'Rice'");
                initializer.execute(isolated);
                assertEquals(users, jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
                assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM food_library", Integer.class));
                assertEquals(777, jdbc.queryForObject("SELECT calories FROM food_library WHERE food_name = 'Rice'", Integer.class));
                assertEquals(9.0, jdbc.queryForObject("SELECT protein FROM food_library WHERE food_name = 'Rice'", Double.class));
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM daily_meal_target", Integer.class));
                assertNotNull(jdbc.queryForObject("SELECT meal_type FROM food_intake LIMIT 1", String.class));
            } finally {
                connection.setCatalog(original);
                connection.createStatement().execute("DROP DATABASE `" + database + "`");
            }
        }
    }
}
