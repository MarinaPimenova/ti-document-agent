package com.wk.ti.question.generation.service;

import com.wk.ti.document.entity.DocumentSection;
import com.wk.ti.question.generation.model.GeneratedQuestion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionGenerationService {

    private final Executor executor;
    private final SectionQuestionGenerator sectionQuestionGenerator;

    public QuestionGenerationService(
            @Qualifier("questionGenerationExecutor")
            Executor executor,
            SectionQuestionGenerator sectionQuestionGenerator) {
        this.executor = executor;
        this.sectionQuestionGenerator = sectionQuestionGenerator;
    }


    public List<GeneratedQuestion> generate(
            List<DocumentSection> sections,
            int requestedQuestionCount,
            String userMessage) {

        Map<Long, Integer> allocation =
                allocateQuestions(sections, requestedQuestionCount);

        List<CompletableFuture<List<GeneratedQuestion>>>
                futures =
                sections.stream()
                        .filter(section ->
                                allocation.containsKey(
                                        section.getSectionId()
                                ))
                        .map(section ->
                                CompletableFuture.supplyAsync(
                                        () -> sectionQuestionGenerator
                                                .generate(section,
                                                        allocation.get(section.getSectionId()), userMessage),
                                        executor))
                        .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList();
    }

    public List<GeneratedQuestion> deduplicate(
            List<GeneratedQuestion> questions) {

        return questions.stream()
                .filter(question ->
                        question.question() != null)
                .collect(Collectors.toMap(
                        question ->
                                question.question()
                                        .trim()
                                        .toLowerCase(),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    public Map<Long, Integer> allocateQuestions(
            List<DocumentSection> sections,
            int requestedQuestionCount) {

        if (sections.isEmpty()) {
            return Map.of();
        }

        int sectionCount = sections.size();

        if (sectionCount >= requestedQuestionCount) {

            return sections.stream()
                    .limit(requestedQuestionCount)
                    .collect(Collectors.toMap(
                            DocumentSection::getSectionId,
                            section -> 1,
                            Integer::sum,
                            LinkedHashMap::new
                    ));
        }

        Map<Long, Integer> allocation =
                new LinkedHashMap<>();

        int base =
                requestedQuestionCount / sectionCount;

        int remainder =
                requestedQuestionCount % sectionCount;

        for (int i = 0; i < sections.size(); i++) {

            int count =
                    base + (i < remainder ? 1 : 0);

            allocation.put(
                    sections.get(i).getSectionId(),
                    count
            );
        }

        return allocation;
    }
}


