package com.ragchat.service;

import com.ragchat.dto.request.CreateProjectRequest;
import com.ragchat.dto.response.ProjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {
    List<ProjectResponse> getAllProjects(String userId);
    ProjectResponse createProject(String userId, CreateProjectRequest request);
    ProjectResponse addContext(String userId, String projectId, String text, MultipartFile image, String videoUrl);
    void deleteProject(String userId, String projectId);
}
