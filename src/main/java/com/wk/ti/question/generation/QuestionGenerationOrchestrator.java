package com.wk.ti.question.generation;

import com.wk.ti.document.entity.DocumentSection;
import com.wk.ti.document.service.DocumentService;
import com.wk.ti.question.generation.model.GeneratedQuestion;
import com.wk.ti.question.generation.model.QuestionGenerationRequest;
import com.wk.ti.question.generation.model.ValidatedQuestion;
import com.wk.ti.question.generation.service.QuestionGenerationService;
import com.wk.ti.question.generation.service.QuestionMetadataValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("UnnecessaryLocalVariable")
@Service
@RequiredArgsConstructor
public class QuestionGenerationOrchestrator {
    private final DocumentService documentService;
    private final QuestionGenerationService questionGenerationService;
    private final QuestionMetadataValidationService metadataValidationService;

    public List<ValidatedQuestion> generate(
            Long documentId, QuestionGenerationRequest questionGenerationRequest) {
        int requestedQuestionCount = questionGenerationRequest.requestedQuestionCount();
        String userMessage = questionGenerationRequest.userMessage();

        // load sections by documentId
        List<DocumentSection> documentSections = documentService.getSections(documentId);
        // generate question (+ enrich by resource information)
        List<GeneratedQuestion> generatedQuestions =
                questionGenerationService.generate(documentSections,
                        requestedQuestionCount,
                        userMessage);
        // deduplicate questions
        List<GeneratedQuestion> deduplicatedQuestions = questionGenerationService.deduplicate(generatedQuestions);
        // validate generated questions
        List<ValidatedQuestion> validatedQuestions = metadataValidationService.validate(deduplicatedQuestions);
        // persist draft

        return validatedQuestions;
    }
}
