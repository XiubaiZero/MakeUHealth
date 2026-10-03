package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.assistant.preferences.AssistantPreferencesService;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.dto.ApiErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/assistant/preferences")
public class AssistantPreferencesController {
    private final AssistantPreferencesService service;
    private final CurrentAccount account;
    public AssistantPreferencesController(AssistantPreferencesService service,CurrentAccount account){this.service=service;this.account=account;}
    @GetMapping public AssistantPreferencesService.Preferences get(){return service.get(account.requireAccountId());}
    public record Update(Boolean enterSendEnabled,Long expectedRevision) { }
    @PatchMapping public AssistantPreferencesService.Preferences update(@RequestBody Update request) {
        int id=account.requireAccountId();
        if(request.enterSendEnabled()==null || request.expectedRevision()==null)throw new IllegalArgumentException("Chat preferences and revision are required.");
        return service.update(id,request.enterSendEnabled(),request.expectedRevision());
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiErrorResponse> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new ApiErrorResponse(e.getReason()));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ApiErrorResponse> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(new ApiErrorResponse(e.getMessage()));}
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<ApiErrorResponse> unavailable(){return ResponseEntity.status(503).body(new ApiErrorResponse("Chat preferences are unavailable. Please retry."));}
}
