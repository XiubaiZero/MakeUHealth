package com.example.ipd_sp_back_end.assistant;

public interface ChatModelClient {
    boolean isConfigured();
    String complete(AssistantPrompt prompt) throws Exception;
    default ChatCompletionResult complete(AssistantPrompt prompt, ChatCompletionOptions options) throws Exception {
        long start = System.nanoTime();
        return new ChatCompletionResult(complete(prompt), "unknown", "unknown", (System.nanoTime()-start)/1_000_000, java.util.Map.of());
    }
}
