package com.ragchat.service.impl;

import com.ragchat.dto.response.MessageResponse;
import com.ragchat.exception.ResourceNotFoundException;
import com.ragchat.model.Message;
import com.ragchat.model.Project;
import com.ragchat.repository.MessageRepository;
import com.ragchat.repository.ProjectRepository;
import com.ragchat.service.ChatService;
import com.ragchat.service.RagPipelineClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final RagPipelineClient ragClient;

    /** Number of recent messages to pass as history to RAG pipeline */
    private static final int HISTORY_WINDOW = 6;

    // ── Fetch message history ──────────────────────────────────────────

    @Override
    public List<MessageResponse> getMessages(String userId, String projectId) {
        verifyProjectOwnership(userId, projectId);
        return messageRepository
                .findByProjectIdAndUserIdOrderByTimestampAsc(projectId, userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Send message ───────────────────────────────────────────────────

    @Override
    public MessageResponse sendMessage(String userId, String projectId, String userMessage) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        // 1. Persist the user's message first
        Message userMsg = Message.builder()
                .projectId(projectId)
                .userId(userId)
                .role(Message.Role.USER)
                .content(userMessage.trim())
                .build();
        messageRepository.save(userMsg);

        // 2. Fetch recent history to send as context to RAG pipeline
        //    (exclude the just-saved user message — we send it as "question")
        List<Message> history = messageRepository
                .findByProjectIdAndUserIdOrderByTimestampAsc(projectId, userId)
                .stream()
                .filter(m -> !m.getId().equals(userMsg.getId()))   // exclude current question
                .collect(Collectors.toList());

        // Trim to last HISTORY_WINDOW messages
        if (history.size() > HISTORY_WINDOW) {
            history = history.subList(history.size() - HISTORY_WINDOW, history.size());
        }

        // 3. Query the RAG pipeline with history context
        String aiAnswer;
        try {
            aiAnswer = ragClient.query(projectId, userMessage, history);
        } catch (Exception e) {
            log.warn("[{}] RAG pipeline unavailable, using fallback response: {}", projectId, e.getMessage());
            aiAnswer = "I received your message, but the AI pipeline is not yet connected. "
                    + "Once the Python RAG service is running, I'll answer based on your uploaded context.";
        }

        // 4. Persist the assistant's response
        Message assistantMsg = Message.builder()
                .projectId(projectId)
                .userId(userId)
                .role(Message.Role.ASSISTANT)
                .content(aiAnswer)
                .build();
        messageRepository.save(assistantMsg);

        // 5. Update dashboard preview (last message shown on project card)
        project.setLastMessage(userMessage.length() > 100
                ? userMessage.substring(0, 97) + "..."
                : userMessage);
        projectRepository.save(project);

        log.info("[{}] Message exchange complete for user [{}]", projectId, userId);
        return toResponse(assistantMsg);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void verifyProjectOwnership(String userId, String projectId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name().toLowerCase())  // "user" | "assistant"
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}
