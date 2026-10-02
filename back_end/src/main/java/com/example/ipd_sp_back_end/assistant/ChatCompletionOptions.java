package com.example.ipd_sp_back_end.assistant;

public record ChatCompletionOptions(double temperature, int maxTokens, int timeoutSeconds, boolean json) {
    public ChatCompletionOptions {
        if (temperature < 0 || temperature > 2 || maxTokens < 1 || timeoutSeconds < 1 || timeoutSeconds > 60)
            throw new IllegalArgumentException("Invalid model options.");
    }
}
