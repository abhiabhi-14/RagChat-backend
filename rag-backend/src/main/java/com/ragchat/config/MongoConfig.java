package com.ragchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables @CreatedDate and @LastModifiedDate auto-population on MongoDB documents.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
