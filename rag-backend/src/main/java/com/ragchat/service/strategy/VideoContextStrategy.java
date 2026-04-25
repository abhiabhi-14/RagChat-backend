package com.ragchat.service.strategy;

import com.ragchat.model.ContextItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handles video URL context — validates the URL format and stores it.
 * The actual transcript extraction is handled by the Python RAG pipeline.
 */
@Slf4j
@Component
public class VideoContextStrategy implements ContextProcessingStrategy {

    @Override
    public ContextItem process(String projectId, String text, MultipartFile file, String videoUrl) {
        validateUrl(videoUrl);
        log.debug("Processing VIDEO context for project [{}], url={}", projectId, videoUrl);

        return ContextItem.builder()
                .type(ContextItem.ContextType.VIDEO)
                .videoUrl(videoUrl.trim())
                .ingested(false)
                .build();
    }

    @Override
    public boolean supports(String text, MultipartFile file, String videoUrl) {
        return videoUrl != null && !videoUrl.isBlank();
    }

    private void validateUrl(String url) {
        try {
            new URL(url.trim());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid video URL: " + url);
        }
    }
}
