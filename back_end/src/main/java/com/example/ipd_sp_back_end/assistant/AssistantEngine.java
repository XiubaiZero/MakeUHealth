package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;

/** Whole-answer boundary; a future Python implementation can use the same contract. */
public interface AssistantEngine {
    AssistantAnswer generate(AssistantChatRequest request, String mode);

    record AssistantAnswer(String answer, String source) { }
}
