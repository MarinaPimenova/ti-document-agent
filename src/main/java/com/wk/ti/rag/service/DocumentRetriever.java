package com.wk.ti.rag.service;

import com.wk.ti.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRetriever {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public List<Document> retrieve(String query) {

        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        RagProperties.Retrieval properties = ragProperties.getRetrieval();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(properties.getTopK())
                .similarityThreshold(properties.getSimilarityThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);

        log.info(
                "RAG retrieval completed. query='{}', requestedTopK={}, retrieved={}",
                query,
                properties.getTopK(),
                documents.size()
        );

        return documents;
    }
}
