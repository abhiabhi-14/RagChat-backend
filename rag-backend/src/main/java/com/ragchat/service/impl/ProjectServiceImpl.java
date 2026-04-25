package com.ragchat.service.impl;

import com.ragchat.dto.request.CreateProjectRequest;
import com.ragchat.dto.response.ProjectResponse;
import com.ragchat.exception.ResourceNotFoundException;
import com.ragchat.model.ContextItem;
import com.ragchat.model.Project;
import com.ragchat.repository.MessageRepository;
import com.ragchat.repository.ProjectRepository;
import com.ragchat.service.ProjectService;
import com.ragchat.service.RagPipelineClient;
import com.ragchat.service.strategy.ContextProcessingStrategy;
import com.ragchat.service.strategy.ContextStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository     projectRepository;
    private final MessageRepository     messageRepository;
    private final ContextStrategyFactory strategyFactory;
    private final RagPipelineClient     ragClient;

    // ── List all projects for a user ───────────────────────────

    @Override
    public List<ProjectResponse> getAllProjects(String userId) {
        return projectRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Create new project ─────────────────────────────────────

    @Override
    public ProjectResponse createProject(String userId, CreateProjectRequest request) {
        Project project = Project.builder()
                .name(request.getName().trim())
                .userId(userId)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Created project [{}] for user [{}]", saved.getId(), userId);
        return toResponse(saved);
    }

    // ── Add context using Strategy pattern ─────────────────────

    @Override
    public ProjectResponse addContext(
            String userId, String projectId,
            String text, MultipartFile image, String videoUrl) {

        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Resolve all applicable strategies (e.g. text + image together)
        List<ContextProcessingStrategy> strategies =
                strategyFactory.resolve(text, image, videoUrl);

        if (strategies.isEmpty()) {
            log.warn("No context strategies matched for project [{}]", projectId);
        }

        // Apply each strategy and collect new ContextItems
        List<ContextItem> newItems = strategies.stream()
                .map(strategy -> strategy.process(projectId, text, image, videoUrl))
                .collect(Collectors.toList());

        project.getContextItems().addAll(newItems);
        Project saved = projectRepository.save(project);

        // Async-friendly: call RAG pipeline to ingest new items
        if (!newItems.isEmpty()) {
            try {
                ragClient.ingestContext(projectId, newItems);
                // Persist ingestion status updates
                projectRepository.save(saved);
            } catch (Exception e) {
                // Pipeline not yet set up — log and continue gracefully
                log.warn("RAG ingestion skipped for project [{}]: {}", projectId, e.getMessage());
            }
        }

        log.info("Added {} context item(s) to project [{}]", newItems.size(), projectId);
        return toResponse(saved);
    }

    // ── Delete project ─────────────────────────────────────────

    @Override
    public void deleteProject(String userId, String projectId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Cascade delete all messages
        messageRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
        log.info("Deleted project [{}] and its messages", projectId);
    }

    // ── DTO builder ────────────────────────────────────────────

    private ProjectResponse toResponse(Project project) {
        List<String> contextTypes = project.getContextItems().stream()
                .map(item -> item.getType().name().toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .contextTypes(contextTypes)
                .lastMessage(project.getLastMessage())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
