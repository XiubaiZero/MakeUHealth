package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.assistant.memory.AssistantMemoryService;
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
    public AssistantMemoryController(AssistantMemoryService memory,CurrentAccount account) { this.memory=memory; this.account=account; }
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
