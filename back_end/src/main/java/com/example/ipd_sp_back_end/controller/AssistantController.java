package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.dto.ApiErrorResponse;
import com.example.ipd_sp_back_end.dto.AssistantChatRequest;
import com.example.ipd_sp_back_end.dto.AssistantChatResponse;
import com.example.ipd_sp_back_end.service.AssistantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AssistantChatRequest request) {
        try {
            String answer = assistantService.ask(request);
            return ResponseEntity.ok(new AssistantChatResponse(answer));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(exception.getMessage()));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(exception.getMessage()));
        }
    }
}

