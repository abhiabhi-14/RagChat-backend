package com.ragchat.service;

import com.ragchat.exception.RagPipelineException;
import com.ragchat.model.ContextItem;
import com.ragchat.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class RagPipelineClient {

    private final RestTemplate restTemplate;
    private final String       baseUrl;

    /** How many recent messages to send as chat history context (last N turns) */
    private static final int HISTORY_WINDOW = 6;

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

    // ── Ingest ─────────────────────────────────────────────────────────

    /**
     * Send all non-ingested context items for a project to the RAG pipeline.
     * Each item is sent in a separate request (text, image, or video).
     * Marks each item as ingested on success.
     */
    public void ingestContext(String projectId, List<ContextItem> contextItems) {
        for (ContextItem item : contextItems) {
            if (item.isIngested()) continue;

            Map<String, Object> payload = buildIngestPayload(projectId, item);

            try {
                log.info("[{}] Ingesting {} context to RAG pipeline", projectId, item.getType());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                        baseUrl + "/ingest", request, Map.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    item.setIngested(true);
                    Object chunks = response.getBody() != null
                            ? response.getBody().get("chunksIngested") : null;
                    log.info("[{}] {} ingested — {} chunk(s)",
                            projectId, item.getType(), chunks != null ? chunks : "?");
                } else {
                    log.warn("[{}] Ingest returned {}", projectId, response.getStatusCode());
                }

            } catch (RestClientException e) {
                // Pipeline might not be up yet — log and continue gracefully
                log.warn("[{}] RAG ingest failed (pipeline may not be running): {}",
                        projectId, e.getMessage());
            }
        }
    }

    // ── Query ──────────────────────────────────────────────────────────

    /**
     * Send a user question + recent chat history to the RAG pipeline.
     * Returns the AI-generated answer string.
     *
     * @param projectId  project providing context
     * @param question   user's current question
     * @param history    recent messages (pass last HISTORY_WINDOW messages)
     */
    public String query(String projectId, String question, List<Message> history) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectId);
        payload.put("question", question);
        payload.put("chatHistory", buildHistoryPayload(history));

        try {
            log.debug("[{}] Querying RAG pipeline: '{}'", projectId, question);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/query", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object answer = response.getBody().get("answer");
                if (answer != null && !answer.toString().isBlank()) {
                    return answer.toString();
                }
            }

            throw new RagPipelineException("RAG pipeline returned an empty response");

        } catch (RestClientException e) {
            log.error("[{}] RAG query failed: {}", projectId, e.getMessage());
            throw new RagPipelineException("Could not reach the AI service: " + e.getMessage());
        }
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    /**
     * Tell the RAG pipeline to delete all vectors for a project.
     * Call this when a project is deleted in Spring Boot.
     */
    public void deleteProjectVectors(String projectId) {
        try {
            restTemplate.delete(baseUrl + "/project/" + projectId);
            log.info("[{}] RAG vectors deleted", projectId);
        } catch (RestClientException e) {
            log.warn("[{}] Could not delete RAG vectors (pipeline may be down): {}",
                    projectId, e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Map<String, Object> buildIngestPayload(String projectId, ContextItem item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectId);
        payload.put("type", item.getType().name());   // "TEXT" | "IMAGE" | "VIDEO"

        switch (item.getType()) {
            case TEXT  -> payload.put("content",  item.getTextContent());
            case IMAGE -> payload.put("filePath", item.getFilePath());
            case VIDEO -> payload.put("videoUrl", item.getVideoUrl());
        }
        return payload;
    }

    /**
     * Convert Spring Boot Message list into the format the Python endpoint expects:
     * [ { "role": "user"|"assistant", "content": "..." }, ... ]
     */
    private List<Map<String, String>> buildHistoryPayload(List<Message> history) {
        if (history == null || history.isEmpty()) return Collections.emptyList();

        // Take last HISTORY_WINDOW messages to avoid token overflow
        List<Message> recent = history.size() > HISTORY_WINDOW
                ? history.subList(history.size() - HISTORY_WINDOW, history.size())
                : history;

        return recent.stream()
                .map(msg -> Map.of(
                        "role",    msg.getRole().name().toLowerCase(),  // "user" | "assistant"
                        "content", msg.getContent()
                ))
                .collect(Collectors.toList());
    }
}
