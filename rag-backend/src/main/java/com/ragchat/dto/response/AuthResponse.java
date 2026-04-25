package com.ragchat.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String name;
        private String email;
    }
}
