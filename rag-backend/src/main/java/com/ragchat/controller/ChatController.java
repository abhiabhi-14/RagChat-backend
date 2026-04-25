package com.ragchat.controller;

import com.ragchat.dto.request.ChatRequest;
import com.ragchat.dto.response.ApiResponse;
import com.ragchat.dto.response.ChatResponse;
import com.ragchat.dto.response.MessageResponse;
import com.ragchat.service.ChatService;
import com.ragchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET  /api/projects/{id}/messages  — Fetch message history
 * POST /api/projects/{id}/chat      — Send a message, get AI response
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService   chatService;
    private final SecurityUtils securityUtils;

    // ── Fetch message history ──────────────────────────────────

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable String projectId) {

        String userId = securityUtils.getCurrentUserId();
        List<MessageResponse> messages = chatService.getMessages(userId, projectId);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    // ── Send message ───────────────────────────────────────────

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @PathVariable String projectId,
            @Valid @RequestBody ChatRequest request) {

        String userId = securityUtils.getCurrentUserId();
        MessageResponse aiMessage = chatService.sendMessage(userId, projectId, request.getMessage());

        // Frontend expects { message: "..." } shape
        ChatResponse response = ChatResponse.builder()
                .message(aiMessage.getContent())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
