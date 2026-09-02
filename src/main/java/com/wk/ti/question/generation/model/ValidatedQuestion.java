package com.wk.ti.question.generation.model;

import com.wk.ti.knowledge.entity.QuestionLevel;
import com.wk.ti.knowledge.entity.Tag;

import java.util.List;

public record ValidatedQuestion(
        String question,
        String answer,
        QuestionLevel level,
        List<Tag> tags,
        List<String> resources // filename:startPage:endPage
) {
}
