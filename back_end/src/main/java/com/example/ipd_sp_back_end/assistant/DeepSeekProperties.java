package com.example.ipd_sp_back_end.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.deepseek")
public class DeepSeekProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-flash";
    private String apiKey = "";
    private double temperature = 0.4;
    private int maxTokens = 4096;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double value) { temperature = value; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int value) { maxTokens = value; }
}
