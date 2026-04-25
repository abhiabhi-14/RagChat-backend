package com.ragchat.service.strategy;

import com.ragchat.model.ContextItem;
import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy Pattern — each context type (TEXT, IMAGE, VIDEO)
 * has its own processing strategy. This interface defines the contract.
 */
public interface ContextProcessingStrategy {

    /**
     * Process the incoming context data and return a persisted ContextItem.
     *
     * @param projectId  the owning project
     * @param text       raw text content (may be null)
     * @param file       uploaded file (may be null)
     * @param videoUrl   video URL (may be null)
     * @return a fully built ContextItem ready to be saved to MongoDB
     */
    ContextItem process(String projectId, String text, MultipartFile file, String videoUrl);

    /**
     * Returns true if this strategy can handle the given inputs.
     */
    boolean supports(String text, MultipartFile file, String videoUrl);
}
