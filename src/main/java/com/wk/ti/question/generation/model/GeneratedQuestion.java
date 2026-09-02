package com.wk.ti.question.generation.model;

import java.util.List;

public record GeneratedQuestion(
        String question,
        String answer,
        String difficultyLevel,
        List<String> tags,
        String resource // filename:startPage:endPage
) {
}
