package com.ragchat.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextItem {

    public enum ContextType {
        TEXT, IMAGE, VIDEO
    }

    private ContextType type;

    /** For TEXT: the raw text content */
    private String textContent;

    /** For IMAGE: relative path on disk (e.g. "uploads/proj123/image.png") */
    private String filePath;

    /** For IMAGE: original filename */
    private String fileName;

    /** For VIDEO: the URL */
    private String videoUrl;

    /** Whether RAG pipeline has ingested this item */
    private boolean ingested;
}
