package com.ragchat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ProjectResponse {

    private String id;
    private String name;
    private List<String> contextTypes;   // ["text", "image", "video"] — lowercase for frontend
    private String lastMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
