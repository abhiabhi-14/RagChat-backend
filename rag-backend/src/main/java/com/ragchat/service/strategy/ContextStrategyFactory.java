package com.ragchat.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Factory Pattern + Strategy Pattern combined.
 *
 * Spring auto-injects all ContextProcessingStrategy implementations.
 * At runtime, the factory iterates through them and returns every
 * strategy that supports the current request inputs — allowing multiple
 * context types to be uploaded at the same time (text + image together).
 */
@Component
@RequiredArgsConstructor
public class ContextStrategyFactory {

    private final List<ContextProcessingStrategy> strategies;

    /**
     * Returns all strategies that can handle the given inputs.
     * Preserves priority order: Text → Image → Video (bean registration order).
     */
    public List<ContextProcessingStrategy> resolve(
            String text, MultipartFile file, String videoUrl) {

        return strategies.stream()
                .filter(s -> s.supports(text, file, videoUrl))
                .toList();
    }
}
