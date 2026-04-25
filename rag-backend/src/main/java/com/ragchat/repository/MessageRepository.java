package com.ragchat.repository;

import com.ragchat.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByProjectIdAndUserIdOrderByTimestampAsc(String projectId, String userId);

    void deleteByProjectId(String projectId);
}
