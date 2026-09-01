package com.wk.ti.rag.service;

import com.wk.ti.rag.dto.AgentPayload;
import com.wk.ti.rag.dto.DocumentAgentResponse;
import com.wk.ti.rag.dto.DocumentSet;
import com.wk.ti.rag.dto.SourceSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;

    private final SimpleLoggerAdvisor simpleLoggerAdvisor;

    public RagService(
            @Qualifier("documentVectorStore")
            VectorStore vectorStore,
            @Qualifier("documentChatMemory")
            ChatMemory chatMemory,
            @Qualifier("openAiChatClient")
            ChatClient chatClient) {
        this.chatClient = chatClient;
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        this.simpleLoggerAdvisor = new SimpleLoggerAdvisor();
        this.messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    public DocumentAgentResponse generate(String conversationId, AgentPayload agentPayload) {

        String question = agentPayload.question();
        Long questionId = agentPayload.questionId();
        Assert.notNull(conversationId, "conversationId cannot be null");
        Assert.notNull(question, "question cannot be null");
        Assert.notNull(questionId, "questionId cannot be null");

        PromptTemplate pt = new PromptTemplate(template);
        Prompt p = pt.create(Map.of("question", question));

        final String activeConversationId = NEW_SESSION_ID.equals(conversationId)
                ? UUID.randomUUID().toString()
                : conversationId;

        String content = chatClient
                .prompt(p)
                .system(systemSpec -> systemSpec.text(systemPrompt)
                        .param("question", question))
                .advisors(advisorSpec -> advisorSpec
                        .advisors(questionAnswerAdvisor, messageChatMemoryAdvisor, simpleLoggerAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId)
                )
                .call()
                .content();

        DocumentAgentResponse finalResponse = DocumentAgentResponse.builder()
                .conversationId(activeConversationId)
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


}