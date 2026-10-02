package com.example.ipd_sp_back_end.dto;

import java.util.List;
import java.util.Map;

public final class AssistantConversationDtos {
    private AssistantConversationDtos() { }
    public record Conversation(String id, String title, long revision, String createdAt, String updatedAt) { }
    public record Message(String id, long sequence, String role, String content, String language,
                          String source, List<String> suggestionPrompts, String createdAt, Map<String,Object> memory) {
        public Message(String id,long sequence,String role,String content,String language,String source,List<String> suggestionPrompts,String createdAt) {
            this(id,sequence,role,content,language,source,suggestionPrompts,createdAt,null);
        }
    }
    public record Task(String id, String requestId, String questionId, String answerId, String status, String error) { }
    public record ConversationPage(List<Conversation> items, boolean hasMore) { }
    public record History(Conversation conversation, List<Message> messages, Long nextBefore, Task activeTask, Task failedTask) { }
    public record RenameRequest(Long expectedRevision, String title) { }
    public record DeleteMessagesRequest(Long expectedRevision, List<String> messageIds) { }
    public record SendRequest(String requestId, Long expectedRevision, String message, String language,
                              Map<String, Object> context, List<String> constraints, String mode, String editMessageId) { }
    public record ImportedMessage(String role, String content, List<String> suggestionPrompts) { }
    public record ImportRequest(List<ImportedMessage> messages, String language) { }
}
