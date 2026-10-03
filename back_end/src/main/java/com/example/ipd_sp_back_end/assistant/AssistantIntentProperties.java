package com.example.ipd_sp_back_end.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="assistant.intent")
public class AssistantIntentProperties {
    private String mode="rules";
    private int recentRounds=3, inputBudget=8192, maxTokens=256, timeoutSeconds=10;
    private double temperature=0;
    public String getMode(){return mode;} public void setMode(String v){mode=v;}
    public int getRecentRounds(){return recentRounds;} public void setRecentRounds(int v){recentRounds=v;}
    public int getInputBudget(){return inputBudget;} public void setInputBudget(int v){inputBudget=v;}
    public int getMaxTokens(){return maxTokens;} public void setMaxTokens(int v){maxTokens=v;}
    public int getTimeoutSeconds(){return timeoutSeconds;} public void setTimeoutSeconds(int v){timeoutSeconds=v;}
    public double getTemperature(){return temperature;} public void setTemperature(double v){temperature=v;}
    public void validate(){
        if (!java.util.Set.of("rules","semantic").contains(mode) || recentRounds<0 || recentRounds>12 || inputBudget<1024 || maxTokens<1 || maxTokens>2048 || timeoutSeconds<1 || timeoutSeconds>30 || !Double.isFinite(temperature) || temperature<0 || temperature>2)
            throw new IllegalArgumentException("Invalid intent configuration.");
    }
}
