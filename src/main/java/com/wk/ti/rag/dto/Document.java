package com.wk.ti.rag.dto;

@SuppressWarnings("unused")
public record Document(
        String id,
        String title,
        String content,
        String source,
        String type,
        Double similarity
) {
    public String getFormat() {
        return """
                - Type: %s
                  Title: %s
                  Source: %s
                  Content: %s
                """.formatted(type, title, source, content);
    }
}

