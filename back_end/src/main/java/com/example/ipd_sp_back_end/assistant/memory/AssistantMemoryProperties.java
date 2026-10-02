package com.example.ipd_sp_back_end.assistant.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="assistant.memory")
public class AssistantMemoryProperties {
    private int recentRounds = 12;
    private int inputBudget = 32768;
    private int contextWindow = 1048576;
    private int summaryMaxTokens = 2048;
    private int extractionMaxTokens = 2048;
    private int auxiliaryTimeoutSeconds = 20;
    private double summaryTemperature = 0;
    private double extractionTemperature = 0;
    private int capacity = 20;
    public int getRecentRounds() { return recentRounds; }
    public void setRecentRounds(int value) { recentRounds = value; }
    public int getInputBudget() { return inputBudget; }
    public void setInputBudget(int value) { inputBudget = value; }
    public int getContextWindow() { return contextWindow; }
    public void setContextWindow(int value) { contextWindow = value; }
    public int getSummaryMaxTokens() { return summaryMaxTokens; }
    public void setSummaryMaxTokens(int value) { summaryMaxTokens = value; }
    public int getExtractionMaxTokens() { return extractionMaxTokens; }
    public void setExtractionMaxTokens(int value) { extractionMaxTokens = value; }
    public int getAuxiliaryTimeoutSeconds() { return auxiliaryTimeoutSeconds; }
    public void setAuxiliaryTimeoutSeconds(int value) { auxiliaryTimeoutSeconds = value; }
    public double getSummaryTemperature() { return summaryTemperature; }
    public void setSummaryTemperature(double value) { summaryTemperature = value; }
    public double getExtractionTemperature() { return extractionTemperature; }
    public void setExtractionTemperature(double value) { extractionTemperature = value; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int value) { capacity = value; }
    public void validate() {
        if (recentRounds < 1 || recentRounds > 100 || inputBudget < 1024 || contextWindow <= inputBudget || summaryMaxTokens < 1 || extractionMaxTokens < 1 || auxiliaryTimeoutSeconds < 1 || auxiliaryTimeoutSeconds > 40 || capacity < 1 || capacity > 100) throw new IllegalArgumentException("Invalid memory configuration.");
    }
}
