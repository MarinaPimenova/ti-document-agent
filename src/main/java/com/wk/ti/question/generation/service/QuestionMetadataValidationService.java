package com.wk.ti.question.generation.service;

import com.wk.ti.question.generation.model.GeneratedQuestion;
import com.wk.ti.question.generation.model.ValidatedQuestion;
import com.wk.ti.knowledge.entity.QuestionLevel;
import com.wk.ti.knowledge.entity.Tag;
import com.wk.ti.knowledge.service.QuestionLevelService;
import com.wk.ti.knowledge.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionMetadataValidationService {

    private final QuestionLevelService questionLevelService;
    private final TagService tagService;

    protected ValidatedQuestion validate(
            GeneratedQuestion candidate) {

        QuestionLevel level =
                questionLevelService
                        .findByCode(candidate.difficultyLevel())
                        .orElseGet(questionLevelService::getDefaultLevel);
//                        .orElseThrow(() ->
//                                new IllegalArgumentException(
//                                        "Unknown difficulty level: " + candidate.difficultyLevel()));

        List<Tag> tags = tagService.findByTagIn(candidate.tags());

        if (tags.size() != candidate.tags().size()) {
            log.error("Generated question contains unknown tags.");
            //throw new IllegalArgumentException("Generated question contains unknown tags.");
            tags = tagService.getDefaultTags();
        }

        return new ValidatedQuestion(
                candidate.question(),
                candidate.answer(),
                level,
                tags,
                List.of(candidate.resource())
        );
    }

    public List<ValidatedQuestion> validate(List<GeneratedQuestion> generatedQuestions) {
        return
                generatedQuestions.stream()
                        .map(this::validate)
                        .toList();

    }
}
