package com.wk.ti.rag.service;


import com.wk.ti.exception.RagGenerationException;
import com.wk.ti.exception.RagNoContextException;
import com.wk.ti.rag.dto.InterviewQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionGenerator {

    private final ChatClient chatClient;
    private final RagPromptBuilder promptBuilder;

    public InterviewQuestionResponse generate(
            String userRequest,
            List<Document> documents,
            int questionCount) {

        if (documents.isEmpty()) {
            throw new RagNoContextException(
                    "No relevant document context was found for the request."
            );
        }

        String systemPrompt = promptBuilder.buildSystemPrompt(
                documents,
                questionCount);

        log.debug(
                "Generating interview questions. questionCount={}, documents={}",
                questionCount,
                documents.size());

        InterviewQuestionResponse response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userRequest)
                .call()
                .entity(InterviewQuestionResponse.class);

        validate(response, questionCount);

        return response;
    }

    private void validate(
            InterviewQuestionResponse response,
            int expectedQuestionCount) {

        if (response == null || response.questions() == null) {
            throw new RagGenerationException(
                    "LLM returned an empty interview question response.");
        }

        if (response.questions().size() != expectedQuestionCount) {
            throw new RagGenerationException(
                    "Expected %d interview questions but received %d."
                            .formatted(
                                    expectedQuestionCount,
                                    response.questions().size()));
        }

        response.questions().forEach(question -> {

            if (question.question() == null
                    || question.question().isBlank()) {

                throw new RagGenerationException(
                        "Generated question cannot be empty.");
            }

            if (question.answer() == null
                    || question.answer().isBlank()) {

                throw new RagGenerationException(
                        "Generated answer cannot be empty.");
            }
        });
    }
}
