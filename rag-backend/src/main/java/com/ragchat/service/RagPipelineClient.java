package com.ragchat.service;

import com.ragchat.exception.RagPipelineException;
import com.ragchat.model.ContextItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that communicates with your Python RAG pipeline.
 *
 * ── Expected Python endpoints ─────────────────────────────────
 *
 * POST /ingest
 *   Body: { "projectId": "...", "type": "TEXT|IMAGE|VIDEO",
 *            "content": "...", "filePath": "...", "videoUrl": "..." }
 *   Response: { "success": true, "message": "..." }
 *
 * POST /query
 *   Body: { "projectId": "...", "question": "..." }
 *   Response: { "answer": "..." }
 *
 * ─────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
public class RagPipelineClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RagPipelineClient(
            RestTemplateBuilder builder,
            @Value("${app.rag.base-url}") String baseUrl,
            @Value("${app.rag.timeout-seconds}") long timeoutSeconds) {

        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.baseUrl = baseUrl;
    }

    /**
     * Sends all context items for a project to the RAG pipeline for ingestion.
     * Called after context is uploaded.
     *
     * @param projectId    the project identifier
     * @param contextItems the list of context items to ingest
     */
    public void ingestContext(String projectId, List<ContextItem> contextItems) {
        for (ContextItem item : contextItems) {
            if (item.isIngested()) continue;   // skip already ingested items

            Map<String, Object> payload = buildIngestPayload(projectId, item);

            try {
                log.info("Ingesting {} context for project [{}]", item.getType(), projectId);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        baseUrl + "/ingest", payload, Map.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Ingestion successful for project [{}] type [{}]",
                            projectId, item.getType());
                    item.setIngested(true);
                } else {
                    log.warn("Ingestion returned non-2xx for project [{}]: {}",
                            projectId, response.getStatusCode());
                }

            } catch (RestClientException e) {
                // Log warning but don't fail the request — pipeline might not be up yet
                log.warn("RAG pipeline ingestion failed for project [{}]: {}", projectId, e.getMessage());
            }
        }
    }

    /**
     * Sends the user's question to the RAG pipeline and returns the AI answer.
     *
     * @param projectId the project providing context
     * @param question  the user's question
     * @return the AI-generated answer
     */
    public String query(String projectId, String question) {
        Map<String, String> payload = Map.of(
                "projectId", projectId,
                "question", question
        );

        try {
            log.debug("Querying RAG pipeline for project [{}]: {}", projectId, question);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/query", payload, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object answer = response.getBody().get("answer");
                if (answer != null) return answer.toString();
            }

            throw new RagPipelineException("RAG pipeline returned an empty response.");

        } catch (RestClientException e) {
            log.error("RAG pipeline query failed for project [{}]: {}", projectId, e.getMessage());
            throw new RagPipelineException("Could not reach the AI service: " + e.getMessage());
        }
    }

    // ── helpers ────────────────────────────────────────────────

    private Map<String, Object> buildIngestPayload(String projectId, ContextItem item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectId);
        payload.put("type", item.getType().name());

        switch (item.getType()) {
            case TEXT  -> payload.put("content", item.getTextContent());
            case IMAGE -> payload.put("filePath", item.getFilePath());
            case VIDEO -> payload.put("videoUrl", item.getVideoUrl());
        }
        return payload;
    }
}
