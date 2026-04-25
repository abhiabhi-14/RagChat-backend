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

    // ── Fetch message history ──────────────────────────────────

    @Override
    public List<MessageResponse> getMessages(String userId, String projectId) {
        // Verify ownership
        verifyProjectOwnership(userId, projectId);

        return messageRepository
                .findByProjectIdAndUserIdOrderByTimestampAsc(projectId, userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Send message ───────────────────────────────────────────

    @Override
    public MessageResponse sendMessage(String userId, String projectId, String userMessage) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // 1. Persist the user's message
        Message userMsg = Message.builder()
                .projectId(projectId)
                .userId(userId)
                .role(Message.Role.USER)
                .content(userMessage.trim())
                .build();
        messageRepository.save(userMsg);

        // 2. Query the RAG pipeline for an AI response
        String aiAnswer;
        try {
            aiAnswer = ragClient.query(projectId, userMessage);
        } catch (Exception e) {
            // Pipeline not set up — return a placeholder response
            log.warn("RAG pipeline unavailable for project [{}], using fallback", projectId);
            aiAnswer = "I received your message, but the AI pipeline is not yet connected. "
                    + "Once your Python RAG service is running, I'll be able to answer based on your context.";
        }

        // 3. Persist the assistant's response
        Message assistantMsg = Message.builder()
                .projectId(projectId)
                .userId(userId)
                .role(Message.Role.ASSISTANT)
                .content(aiAnswer)
                .build();
        messageRepository.save(assistantMsg);

        // 4. Update the project's lastMessage preview (truncated to 100 chars)
        project.setLastMessage(userMessage.length() > 100
                ? userMessage.substring(0, 97) + "..."
                : userMessage);
        projectRepository.save(project);

        return toResponse(assistantMsg);
    }

    // ── Helpers ────────────────────────────────────────────────

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
