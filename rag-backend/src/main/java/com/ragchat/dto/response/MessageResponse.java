package com.ragchat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageResponse {

    private String id;
    private String role;        // "user" or "assistant" — lowercase for frontend
    private String content;
    private Instant timestamp;
}
