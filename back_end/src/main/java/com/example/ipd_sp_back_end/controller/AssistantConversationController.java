package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.dto.ApiErrorResponse;
import com.example.ipd_sp_back_end.dto.AssistantConversationDtos.*;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.AssistantConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assistant/conversations")
public class AssistantConversationController {
    private final AssistantConversationService service;
    private final CurrentAccount account;
    public AssistantConversationController(AssistantConversationService service, CurrentAccount account) { this.service = service; this.account = account; }
    @GetMapping public ConversationPage list(@RequestParam(defaultValue="0") int offset, @RequestParam(defaultValue="20") int limit) { return service.list(account.requireAccountId(), offset, limit); }
    @PostMapping public Conversation create() { return service.create(account.requireAccountId()); }
    @GetMapping("/{id}/messages") public History history(@PathVariable String id, @RequestParam(required=false) Long before, @RequestParam(defaultValue="80") int limit) { return service.history(account.requireAccountId(), id, before, limit); }
    @PatchMapping("/{id}") public Conversation rename(@PathVariable String id, @RequestBody RenameRequest request) { return service.rename(account.requireAccountId(), id, request); }
    @DeleteMapping("/{id}") public void delete(@PathVariable String id, @RequestParam Long expectedRevision) { service.delete(account.requireAccountId(), id, expectedRevision); }
    @PostMapping("/{id}/messages/delete") public void deleteMessages(@PathVariable String id, @RequestBody DeleteMessagesRequest request) { service.deleteMessages(account.requireAccountId(), id, request); }
    @PostMapping("/{id}/turns") public ResponseEntity<Task> send(@PathVariable String id, @RequestBody SendRequest request) {
        Task task = service.send(account.requireAccountId(), id, request);
        return ResponseEntity.status("queued".equals(task.status()) || "running".equals(task.status()) ? 202 : 200).body(task);
    }
    @GetMapping("/{id}/turns/{requestId}") public Task task(@PathVariable String id, @PathVariable String requestId) { return service.task(account.requireAccountId(), id, requestId); }
    @PostMapping("/import") public Conversation importHistory(@RequestBody ImportRequest request) { return service.importHistory(account.requireAccountId(), request); }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiErrorResponse> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiErrorResponse(exception.getReason()));
    }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<ApiErrorResponse> unavailable() {
        return ResponseEntity.status(503).body(new ApiErrorResponse("Chat storage is unavailable. Your draft has been kept."));
    }
}
