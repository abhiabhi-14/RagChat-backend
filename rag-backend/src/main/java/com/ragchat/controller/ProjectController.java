package com.ragchat.controller;

import com.ragchat.dto.request.CreateProjectRequest;
import com.ragchat.dto.response.ApiResponse;
import com.ragchat.dto.response.ProjectResponse;
import com.ragchat.service.ProjectService;
import com.ragchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * GET    /api/projects                    — List all projects
 * POST   /api/projects                    — Create project
 * DELETE /api/projects/{id}               — Delete project
 * POST   /api/projects/{id}/context       — Upload context (text/image/video)
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService  projectService;
    private final SecurityUtils   securityUtils;

    // ── List all projects ──────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects() {
        String userId = securityUtils.getCurrentUserId();
        List<ProjectResponse> projects = projectService.getAllProjects(userId);
        return ResponseEntity.ok(ApiResponse.ok(projects));
    }

    // ── Create project ─────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {

        String userId = securityUtils.getCurrentUserId();
        ProjectResponse project = projectService.createProject(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Project created", project));
    }

    // ── Delete project ─────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteProject(
            @PathVariable String id) {

        String userId = securityUtils.getCurrentUserId();
        projectService.deleteProject(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
    }

    // ── Add context (multipart form) ───────────────────────────
    // Accepts any combination of: text, image file, and/or video URL

    @PostMapping("/{id}/context")
    public ResponseEntity<ApiResponse<ProjectResponse>> addContext(
            @PathVariable String id,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String videoUrl) {

        String userId = securityUtils.getCurrentUserId();
        ProjectResponse updated = projectService.addContext(userId, id, text, image, videoUrl);
        return ResponseEntity.ok(ApiResponse.ok("Context added successfully", updated));
    }
}
