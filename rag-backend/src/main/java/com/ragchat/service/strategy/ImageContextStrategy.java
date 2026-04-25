package com.ragchat.service.strategy;

import com.ragchat.exception.FileStorageException;
import com.ragchat.model.ContextItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Handles image uploads — saves to disk under uploads/{projectId}/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageContextStrategy implements ContextProcessingStrategy {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public ContextItem process(String projectId, String text, MultipartFile file, String videoUrl) {
        validateFile(file);

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        Path   dir      = Paths.get(uploadDir, projectId);
        Path   dest     = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            file.transferTo(dest);
            log.debug("Saved image to [{}]", dest);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store image: " + e.getMessage());
        }

        return ContextItem.builder()
                .type(ContextItem.ContextType.IMAGE)
                .filePath(dest.toString())
                .fileName(file.getOriginalFilename())
                .ingested(false)
                .build();
    }

    @Override
    public boolean supports(String text, MultipartFile file, String videoUrl) {
        return file != null && !file.isEmpty();
    }

    // ── helpers ────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "Unsupported file type: " + contentType + ". Allowed: JPEG, PNG, WEBP, GIF");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new FileStorageException("File too large. Maximum size is 20 MB.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
