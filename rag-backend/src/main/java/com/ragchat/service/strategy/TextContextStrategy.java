package com.ragchat.service.strategy;

import com.ragchat.model.ContextItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles plain text context — just validates and stores the content.
 */
@Slf4j
@Component
public class TextContextStrategy implements ContextProcessingStrategy {

    @Override
    public ContextItem process(String projectId, String text, MultipartFile file, String videoUrl) {
        log.debug("Processing TEXT context for project [{}], length={}", projectId, text.length());

        return ContextItem.builder()
                .type(ContextItem.ContextType.TEXT)
                .textContent(text.trim())
                .ingested(false)
                .build();
    }

    @Override
    public boolean supports(String text, MultipartFile file, String videoUrl) {
        return text != null && !text.isBlank();
    }
}
