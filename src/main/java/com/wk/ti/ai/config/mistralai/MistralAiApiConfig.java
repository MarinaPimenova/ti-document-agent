package com.wk.ti.ai.config.mistralai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.ai.mistralai.MistralAiEmbeddingOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MistralAiApiConfig {
    private final MistralAiConfig mistralAiConfig;

    @Bean
    public MistralAiApi mistralAiApi() {
        return MistralAiApi.builder()
                .apiKey(mistralAiConfig.getApiKey())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(MistralAiApi mistralAiApi) {

        MistralAiEmbeddingOptions options = MistralAiEmbeddingOptions.builder()
                .withModel("mistral-embed")
                .withEncodingFormat("float")
                .build();
        return MistralAiEmbeddingModel.builder()
                .mistralAiApi(mistralAiApi)
                .options(options)
                .build();
    }
}
