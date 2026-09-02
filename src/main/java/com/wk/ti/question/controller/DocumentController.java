package com.wk.ti.question.controller;

import com.wk.ti.document.entity.DocumentProjection;
import com.wk.ti.document.service.DocumentService;

import com.wk.ti.question.generation.QuestionGenerationOrchestrator;
import com.wk.ti.question.generation.model.QuestionGenerationRequest;
import com.wk.ti.question.generation.model.ValidatedQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final QuestionGenerationOrchestrator questionGenerationOrchestrator;

    @GetMapping(value = "/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentProjection>> getDocuments() {
        return ResponseEntity.ok(documentService.getDocuments());
    }

    @PostMapping(value = "/documents/{id}/question-generation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ValidatedQuestion>> generate(
            @RequestBody QuestionGenerationRequest questionGenerationRequest,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(questionGenerationOrchestrator.generate(
                id, questionGenerationRequest));
    }
}
