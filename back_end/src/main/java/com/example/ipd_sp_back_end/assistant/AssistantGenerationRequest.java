package com.example.ipd_sp_back_end.assistant;

import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.assistant.memory.MemorySnapshot;

/** Authorized data is assembled by Java; an engine never directly owns chat storage. */
public record AssistantGenerationRequest(AssistantChatRequest request, String mode, MemorySnapshot memory) { }
