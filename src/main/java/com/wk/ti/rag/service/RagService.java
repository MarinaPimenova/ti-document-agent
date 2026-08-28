package com.wk.ti.rag.service;

import com.wk.ti.api.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RagService {
    private static final String NEW_SESSION_ID = "1";

    private final String template = """
            You're assisting with questions.
            Use the following context and chat history to answer the QUESTION but act as if you knew this information innately.
            If unsure, simply state that you don't know.
            
            QUESTION
            {question}
            
            """;
    @Value("classpath:/system-prompt-template.st")
    private Resource systemPrompt;
    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final SimpleLoggerAdvisor simpleLoggerAdvisor;

    public RagService(
            VectorStore vectorStore,
            ChatMemory chatMemory, ChatClient chatClient) {
        this.chatMemory = chatMemory;
        this.chatClient = chatClient;
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        this.simpleLoggerAdvisor = new SimpleLoggerAdvisor();
    }

    public DocumentAgentResponse generate(String conversationId, AgentPayload agentPayload) {

        String question = agentPayload.question();
        Long questionId = agentPayload.questionId();
        Assert.notNull(conversationId, "conversationId cannot be null");
        Assert.notNull(question, "question cannot be null");
        Assert.notNull(questionId, "questionId cannot be null");

        PromptTemplate pt = new PromptTemplate(template);
        Prompt p = pt.create(Map.of("question", question));
        if (NEW_SESSION_ID.equals(conversationId)) {
            conversationId = UUID.randomUUID().toString();
        }

        String content = chatClient
                .prompt(p)
                .system(systemSpec -> systemSpec.text(systemPrompt)
                        .param("question", question))
                .advisors(
                        promptChatMemoryAdvisor(conversationId),
                        questionAnswerAdvisor,
                        simpleLoggerAdvisor)
                .call()
                .content();

        DocumentAgentResponse finalResponse = DocumentAgentResponse.builder()
                .conversationId(conversationId)
                .questionId(questionId)
                .termList(question)
                .sourceSet(SourceSet.fallbackSummary())
                .documentSet(DocumentSet.of(List.of()))
                .summary(content)
                .build();
        log.info("Document Agent: final step: final response was generated. QuestionId: {}, question: {}, Summary: {}",
                questionId, question, finalResponse.getSummary());
        return finalResponse;
    }

    protected PromptChatMemoryAdvisor promptChatMemoryAdvisor(String conversationId) {
        return PromptChatMemoryAdvisor
                .builder(chatMemory)
                .conversationId(conversationId)
                .build();
    }
}


