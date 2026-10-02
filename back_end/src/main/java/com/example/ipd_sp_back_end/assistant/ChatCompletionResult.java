package com.example.ipd_sp_back_end.assistant;

import java.util.Map;

public record ChatCompletionResult(String text, String model, String finishReason, long elapsedMillis, Map<String,Object> usage) {
    public ChatCompletionResult { usage = usage == null ? Map.of() : Map.copyOf(usage); }
}
