package com.ragchat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    public enum Role {
        USER, ASSISTANT
    }

    @Id
    private String id;

    @NonNull
    @Indexed
    private String projectId;

    @NonNull
    @Indexed
    private String userId;

    @NonNull
    private Role role;

    @NonNull
    private String content;

    @CreatedDate
    private Instant timestamp;
}
