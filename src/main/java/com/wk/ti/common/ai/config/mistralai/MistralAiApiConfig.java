package com.wk.ti.common.ai.config.mistralai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
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

        return MistralAiEmbeddingModel.builder()
                .mistralAiApi(mistralAiApi)
                .build();
    }
}
