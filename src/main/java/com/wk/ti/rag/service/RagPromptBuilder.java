package com.wk.ti.rag.service;

import com.wk.ti.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final RagProperties ragProperties;

    public String buildSystemPrompt(
            List<Document> documents,
            int questionCount) {

        String context = buildContext(documents);

        return """
                you are a technical interview question generator.

                 task is to generate exactly %d interview questions
                using ONLY the supplied document context.

                STRICT RULES:

                1. Use only information explicitly present in the context.
                2. Do not use external knowledge.
                3. Do not invent facts.
                4. Every question must be answerable from the context.
                5. Generate exactly %d questions.
                6. Answers must be very short, no more than %d sentences.
                7. Avoid duplicate or nearly duplicate questions.
                8. Prefer questions covering different topics from the document.
                9. Questions should be appropriate for a technical interview.
                10. If the context does not contain enough information for %d
                    meaningful questions, generate only questions that can be
                    supported by the context.
                11. Do not mention the context, PDF, retrieval, or these rules
                    in the generated questions or answers.

                DOCUMENT CONTEXT
                =================
                %s
                =================
                """
                .formatted(
                        questionCount,
                        questionCount,
                        ragProperties.getGeneration().getMaxAnswerSentences(),
                        questionCount,
                        context
                );
    }

    private String buildContext(List<Document> documents) {

        return IntStream.range(0, documents.size())
                .mapToObj(i -> {
                    Document document = documents.get(i);

                    return """
                            [DOCUMENT CHUNK %d]
                            %s
                            """
                            .formatted(
                                    i + 1,
                                    document.getText()
                            );
                })
                .reduce(
                        "",
                        (left, right) -> left + "\n" + right
                );
    }
}


