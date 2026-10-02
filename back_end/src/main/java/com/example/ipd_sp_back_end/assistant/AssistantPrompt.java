package com.example.ipd_sp_back_end.assistant;


public record AssistantPrompt(String systemPrompt, String userPrompt, java.util.List<Message> history) {
    public AssistantPrompt(String systemPrompt, String userPrompt) { this(systemPrompt, userPrompt, java.util.List.of()); }
    public AssistantPrompt { history = java.util.List.copyOf(history); }
    public record Message(String role, String content) {
        public Message {
            if (!("user".equals(role) || "assistant".equals(role)) || content == null) throw new IllegalArgumentException("Invalid history message.");
        }
    }
}
