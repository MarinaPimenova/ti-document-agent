package com.wk.ti.api.dto;

@SuppressWarnings("unused")
public record Document(
        String id,
        String title,
        String content,
        String studyName,
        String revOpsId,
        String source,
        String type,
        Double similarity
) {
    public String getFormat() {
        return """
                - Type: %s
                  Title: %s
                  StudyName: %s
                  RevOpsId: %s
                  URL: %s
                  Content: %s
                """.formatted(type, title, studyName, revOpsId, source, content);
    }
}

