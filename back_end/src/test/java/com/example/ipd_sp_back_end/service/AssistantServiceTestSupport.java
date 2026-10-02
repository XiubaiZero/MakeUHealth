package com.example.ipd_sp_back_end.service;

import java.net.http.HttpClient;
import org.springframework.test.util.ReflectionTestUtils;

final class AssistantServiceTestSupport {
    private AssistantServiceTestSupport() { }
    static AssistantService unconfigured() { return new AssistantService(); }
    static AssistantService configured(String url) {
        AssistantService service = unconfigured();
        ReflectionTestUtils.setField(service, "deepseekApiKey", "test-only-key");
        ReflectionTestUtils.setField(service, "deepseekBaseUrl", url);
        ReflectionTestUtils.setField(service, "deepseekModel", "test-model");
        return service;
    }
    static AssistantService withHttpClient(HttpClient client) {
        AssistantService service = configured("http://127.0.0.1:1");
        ReflectionTestUtils.setField(service, "httpClient", client);
        return service;
    }
}
