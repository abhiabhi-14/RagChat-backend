package com.ragchat.exception;

// Re-export with public visibility so services can use them directly

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
