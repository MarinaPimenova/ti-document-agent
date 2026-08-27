package com.wk.ti.ai.config.anthropicai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wk.ti.ai.config.AIConfig;
import com.wk.ti.exception.ImageHandlerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component

public class OpenAiVisionClient {

    private final AIConfig aiConfig;
    private final String imageModel;
    private final String completionsPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiVisionClient(AIConfig aiConfig,
                              @Value("${openai.image-model}") String imageModel,
                              @Value("${openai.image-completions-path}") String completionsPath) {
        this.aiConfig = aiConfig;
        this.imageModel = imageModel;
        this.completionsPath = completionsPath;
    }

    public String ask(List<Map<String, Object>> content) {
        String apiKey = aiConfig.getApiKey();

        String baseUrl = aiConfig.getBaseUrl();

        try {
            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", content
            );
            Map<String, Object> requestBody = Map.of(
                    "model", imageModel,
                    "messages", List.of(message),
                    "max_tokens", 500
            );
            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + completionsPath))
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() == HttpStatus.OK.value()) {
                return objectMapper.readTree(response.body())
                        .path("choices").get(0).path("message").path("content").asText();
            } else {
                throw new ImageHandlerException(response.body() + response.statusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("OpenAI Vision API call failed", e);
        }
    }
}
