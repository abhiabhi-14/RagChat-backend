package com.ragchat.service;

import com.ragchat.dto.response.MessageResponse;

import java.util.List;

public interface ChatService {
    List<MessageResponse> getMessages(String userId, String projectId);
    MessageResponse sendMessage(String userId, String projectId, String userMessage);
}
