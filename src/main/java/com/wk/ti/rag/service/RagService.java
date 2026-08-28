package com.wk.ti.rag.service;

import com.wk.ti.api.dto.AgentPayload;
import com.wk.ti.api.dto.DocumentAgentResponse;
import com.wk.ti.api.dto.DocumentSet;
import com.wk.ti.api.dto.SourceSet;
import com.wk.ti.exception.RagNoContextException;
import com.wk.ti.rag.config.RagProperties;
import com.wk.ti.rag.dto.GenerateInterviewRequest;
import com.wk.ti.rag.dto.InterviewQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

import static com.wk.ti.api.dto.DocumentAgentResponse.toSummary;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentRetriever documentRetriever;
    private final InterviewQuestionGenerator questionGenerator;
    private final RagProperties ragProperties;

    public DocumentAgentResponse /*InterviewQuestionResponse*/ generate(
            String conversationId,
            AgentPayload agentPayload) {
        GenerateInterviewRequest request = new GenerateInterviewRequest(agentPayload.question(), 10);
        Assert.notNull(request, "request cannot be null");

        String userQuestion = request.question();

        Assert.hasText(
                userQuestion,
                "question cannot be null or empty"
        );

        int questionCount = request.questionCountOrDefault(
                ragProperties.getGeneration().getDefaultQuestionCount()
        );

        validateQuestionCount(questionCount);

        log.info(
                "Starting RAG generation. question='{}', questionCount={}",
                userQuestion,
                questionCount
        );

        List<Document> documents =
                documentRetriever.retrieve(userQuestion);

        if (documents.isEmpty()) {
            throw new RagNoContextException(
                    "No relevant information was found in the uploaded documents."
            );
        }

        InterviewQuestionResponse response = questionGenerator.generate(
                userQuestion,
                documents,
                questionCount);
        return DocumentAgentResponse.builder()
                .conversationId(conversationId)
                .questionId(agentPayload.questionId())
                .summary(toSummary(response))
                .sourceSet(SourceSet.fallbackSummary())
                .documentSet(DocumentSet.of(List.of()))
                .termList(agentPayload.question())
                .build();
    }

    private void validateQuestionCount(int questionCount) {

        if (questionCount < 1
                || questionCount > ragProperties.getGeneration().getMaxQuestionCount()) {

            throw new IllegalArgumentException(
                    "Question count must be between 1 and "
                            + ragProperties.getGeneration().getMaxQuestionCount()
            );
        }
    }
}