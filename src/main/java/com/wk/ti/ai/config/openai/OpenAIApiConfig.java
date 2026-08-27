package com.wk.ti.ai.config.openai;

import com.wk.ti.ai.config.AIConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIApiConfig {
    @Qualifier("openAIConfig")
    private final OpenAIConfig aiConfig;

    public OpenAIApiConfig(OpenAIConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Bean
    public OpenAiApi openAiApi(
            @Qualifier("llmRestClientBuilder") RestClient.Builder llmRestClientBuilder,
            WebClient.Builder webClientBuilder) {

        DefaultResponseErrorHandler responseErrorHandler = new DefaultResponseErrorHandler();

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Api-Key", aiConfig.getApiKey());
        headers.add("cache-control", "no-cache");

        ApiKey apiKey = new SimpleApiKey(aiConfig.getApiKey());

        return new OpenAiApi(
                aiConfig.getBaseUrl(),
                apiKey,
                headers,
                aiConfig.getChat().getCompletionsPath(),
                aiConfig.getChat().getEmbeddingsPath(),
                llmRestClientBuilder,
                webClientBuilder,
                responseErrorHandler
        );
    }

    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        AIConfig.Options opts = aiConfig.getChat().getOptions();

        String model = (opts != null && opts.getModel() != null && !opts.getModel().isBlank())
                ? opts.getModel()
                : aiConfig.getChat().getModel();

        OpenAiChatOptions.Builder b = OpenAiChatOptions.builder()
                .model(model)
                .streamUsage(opts != null && Boolean.TRUE.equals(opts.getStreamUsage()));

        // Only set temperature when configured (non-null) so the field is omitted otherwise.
        if (opts != null && opts.getTemperature() != null) {
            b.temperature(opts.getTemperature());
        }

        OpenAiChatOptions openAiChatOptions = b.build();

        return OpenAiChatModel.builder()
                .defaultOptions(openAiChatOptions)
                .openAiApi(openAiApi)
                .build();
    }

    @Bean
    public ChatClient openAiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}