package com.example.ipd_sp_back_end.assistant;

public interface ChatModelClient {
    boolean isConfigured();
    String complete(AssistantPrompt prompt) throws Exception;
}
