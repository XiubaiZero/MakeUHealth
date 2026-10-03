package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.assistant.memory.MemorySnapshot;
import java.util.List;

public record AssistantIntentInput(String question, String language, String summary, List<MemorySnapshot.Round> rounds) {
    public AssistantIntentInput { rounds = List.copyOf(rounds); }
    public static AssistantIntentInput from(String question, String language, MemorySnapshot memory) {
        return new AssistantIntentInput(question, language, memory.enabled() ? memory.summary() : "", memory.enabled() ? memory.rounds() : List.of());
    }
}
