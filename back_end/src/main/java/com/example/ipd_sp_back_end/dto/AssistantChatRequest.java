package com.example.ipd_sp_back_end.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AssistantChatRequest {
    private String message;
    private String language;
    private Map<String, Object> context;
    private List<String> constraints;
}

