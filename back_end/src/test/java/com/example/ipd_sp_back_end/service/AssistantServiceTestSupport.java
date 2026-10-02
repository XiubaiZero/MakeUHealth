package com.example.ipd_sp_back_end.service;

import java.net.http.HttpClient;
import com.example.ipd_sp_back_end.assistant.*;

final class AssistantServiceTestSupport {
    private AssistantServiceTestSupport() { }
    static AssistantService service(ChatModelClient client) {
        return new AssistantService(new RuleBasedAssistantIntentClassifier(), new AssistantReplyCatalog(),
                new AssistantContextFormatter(), new AssistantPromptBuilder(), client, new AssistantResponseProcessor());
    }
    static AssistantService unconfigured() { return service(new DeepSeekChatClient(new DeepSeekProperties())); }
    static DeepSeekProperties properties(String url) {
        var properties = new DeepSeekProperties();
        properties.setApiKey("test-only-key"); properties.setBaseUrl(url); properties.setModel("test-model");
        return properties;
    }
    static AssistantService configured(String url) { return service(new DeepSeekChatClient(properties(url))); }
    static AssistantService withHttpClient(HttpClient client) {
        return service(new DeepSeekChatClient(properties("http://127.0.0.1:1"), client));
    }
}
