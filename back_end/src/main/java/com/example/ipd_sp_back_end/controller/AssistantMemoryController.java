package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.assistant.memory.AssistantMemoryService;
import com.example.ipd_sp_back_end.assistant.memory.AssistantPersonalMemoryService;
import com.example.ipd_sp_back_end.assistant.memory.AssistantMemoryExtractionService;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assistant")
public class AssistantMemoryController {
    private final AssistantMemoryService memory;
    private final CurrentAccount account;
    private final AssistantPersonalMemoryService personal;
    private final AssistantMemoryExtractionService extraction;
    public AssistantMemoryController(AssistantMemoryService memory,CurrentAccount account,AssistantPersonalMemoryService personal,AssistantMemoryExtractionService extraction) { this.memory=memory; this.account=account;this.personal=personal;this.extraction=extraction; }
    @GetMapping("/memory") public AssistantPersonalMemoryService.State state() { return personal.state(account.requireAccountId()); }
    @PatchMapping("/memory") public AssistantPersonalMemoryService.AccountSettings accountToggle(@RequestBody ToggleRequest request) {
        if(request.expectedRevision()==null || request.enabled()==null) throw new IllegalArgumentException("Memory settings and revision are required.");
        return personal.toggle(account.requireAccountId(),request.expectedRevision(),request.enabled());
    }
    @PostMapping("/memory/items") public AssistantPersonalMemoryService.Item add(@RequestBody AssistantPersonalMemoryService.Change request) { return personal.add(account.requireAccountId(),request); }
    @PatchMapping("/memory/items/{id}") public AssistantPersonalMemoryService.Item change(@PathVariable String id,@RequestBody AssistantPersonalMemoryService.Change request) { return personal.change(account.requireAccountId(),id,request); }
    @DeleteMapping("/memory/items/{id}") public ResponseEntity<Void> delete(@PathVariable String id,@RequestParam long expectedRevision) { personal.delete(account.requireAccountId(),id,expectedRevision);return ResponseEntity.noContent().build(); }
    @DeleteMapping("/memory") public ResponseEntity<Void> clear(@RequestParam long expectedRevision) { personal.clear(account.requireAccountId(),expectedRevision);return ResponseEntity.noContent().build(); }
    @PostMapping("/conversations/{id}/memory/extractions") public ResponseEntity<AssistantMemoryExtractionService.Job> extract(@PathVariable String id,@RequestBody AssistantMemoryExtractionService.Request request) { return ResponseEntity.accepted().body(extraction.create(account.requireAccountId(),id,request)); }
    @GetMapping("/conversations/{id}/memory/extractions/{requestId}") public AssistantMemoryExtractionService.Job extraction(@PathVariable String id,@PathVariable String requestId) { return extraction.get(account.requireAccountId(),id,requestId); }
    public record ToggleRequest(Long expectedRevision,Boolean enabled) { }
    @GetMapping("/conversations/{id}/memory") public AssistantMemoryService.Settings settings(@PathVariable String id) { return memory.settings(account.requireAccountId(),id); }
    @PatchMapping("/conversations/{id}/memory") public AssistantMemoryService.Settings toggle(@PathVariable String id,@RequestBody ToggleRequest request) {
        if (request.expectedRevision()==null || request.enabled()==null) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Memory settings and revision are required.");
        return memory.toggle(account.requireAccountId(),id,request.expectedRevision(),request.enabled());
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiErrorResponse> error(ResponseStatusException exception) { return ResponseEntity.status(exception.getStatusCode()).body(new ApiErrorResponse(exception.getReason())); }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<ApiErrorResponse> unavailable() { return ResponseEntity.status(503).body(new ApiErrorResponse("Memory storage is unavailable. Please retry.")); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ApiErrorResponse> invalid(IllegalArgumentException exception) { return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage())); }
}
