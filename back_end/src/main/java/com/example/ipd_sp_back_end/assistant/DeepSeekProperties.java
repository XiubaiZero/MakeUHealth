package com.example.ipd_sp_back_end.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.deepseek")
public class DeepSeekProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private String apiKey = "";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
