package com.wk.ti.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Retrieval retrieval = new Retrieval();
    private Chunking chunking = new Chunking();
    private Generation generation = new Generation();

    @Getter
    @Setter
    public static class Retrieval {

        private int topK;
        private double similarityThreshold;
    }

    @Getter
    @Setter
    public static class Chunking {

        private int chunkSize;
        private int minChunkSizeChars;
        private int minChunkLengthToEmbed;
        private int maxNumChunks;
    }

    @Getter
    @Setter
    public static class Generation {

        private int defaultQuestionCount;
        private int maxQuestionCount;
        private int maxAnswerSentences;
    }
}