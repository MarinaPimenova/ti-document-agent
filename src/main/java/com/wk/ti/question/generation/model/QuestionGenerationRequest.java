package com.wk.ti.question.generation.model;

public record QuestionGenerationRequest(
        String userMessage,
        int requestedQuestionCount
) {
}
