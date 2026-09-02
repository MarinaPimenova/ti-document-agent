package com.wk.ti.question.generation.service;

import com.wk.ti.document.entity.DocumentSection;
import com.wk.ti.question.generation.model.GeneratedQuestion;
import com.wk.ti.question.generation.model.GeneratedQuestionBatch;
import com.wk.ti.knowledge.service.QuestionLevelService;
import com.wk.ti.knowledge.service.TagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
@Service
public class SectionQuestionGenerator {
    private final static String USER_MESSAGE = """
            You're assisting with generating technical interview questions.
            Use the following context and chat history to generate them.
            Only if MESSAGE is relevant for the technical questions generation then take it into account.
            
            MESSAGE
            {message}
            
            """;
    private final static String SYSTEM_PROMPT = """
            You are generating technical interview questions.
            
            Generate exactly {count} questions based ONLY on the supplied
            document section.
            
            Requirements:
            
            - Questions must be suitable for a technical interview.
            - Questions must be answerable from the supplied content.
            - Do not introduce external knowledge.
            - Do not duplicate concepts within this section.
            - Answers must be short.
            - Each answer should contain at most 2-3 sentences.
            - Prefer conceptual and practical questions.
            - Do not mention the document or this prompt.
            - Assign the difficulty level to each question using the following possible values: {levels}.
            - Assign the tags to each question using the following possible values: {tags}.
            - Assign the resource to each question using the following format: {filename}:{startPage}:{endPage}.
            
            DOCUMENT SECTION:
            
            {section}
            """;

    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;
    private final SimpleLoggerAdvisor simpleLoggerAdvisor;
    private final QuestionLevelService questionLevelService;
    private final TagService tagService;

    public SectionQuestionGenerator(
            @Qualifier("documentChatMemory")
            ChatMemory chatMemory,
            @Qualifier("openAiChatClient")
            ChatClient chatClient, QuestionLevelService questionLevelService, TagService tagService) {
        this.chatClient = chatClient;
        this.questionLevelService = questionLevelService;
        this.tagService = tagService;
        this.simpleLoggerAdvisor = new SimpleLoggerAdvisor();
        this.messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    public List<GeneratedQuestion> generate(
            DocumentSection documentSection,
            int requestedQuestionCount,
            String userMessage
    ) {
        String levels = questionLevelService.levels();
        String tags = tagService.tags();

        PromptTemplate pt = new PromptTemplate(USER_MESSAGE);
        Prompt p = pt.create(Map.of("message", userMessage));
        try {
            GeneratedQuestionBatch result =
                    chatClient.prompt(p)
                            .system(systemSpec -> systemSpec.text(SYSTEM_PROMPT)
                                    .param("count", requestedQuestionCount)
                                    .param("levels", levels)
                                    .param("tags", tags)
                                    .param("filename", documentSection.getFilename())
                                    .param("startPage", documentSection.getStartPage())
                                    .param("endPage", documentSection.getEndPage())
                                    .param("section", documentSection.getContent())
                            )
                            .advisors(advisorSpec -> advisorSpec
                                    .advisors(simpleLoggerAdvisor)
                            )
                            //.user(documentSection.getContent())
                            .call()
                            .entity(GeneratedQuestionBatch.class);
            return result.questions();
        } catch (Exception e) {
            return List.of();
        }
    }
}
