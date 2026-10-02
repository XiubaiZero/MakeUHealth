package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;

/** Whole-answer boundary; a future Python implementation can use the same contract. */
public interface AssistantEngine {
    AssistantAnswer generate(AssistantChatRequest request, String mode);
    default AssistantAnswer generate(AssistantGenerationRequest request) { return generate(request.request(), request.mode()); }
    default void validate(AssistantChatRequest request) { }
    default void validate(AssistantGenerationRequest request) { validate(request.request()); }

    record AssistantAnswer(String answer, String source, java.util.Map<String,Object> memory, String summaryUpdate, long summaryThrough) {
        public AssistantAnswer(String answer,String source) { this(answer,source,java.util.Map.of(),null,0); }
    }
}
