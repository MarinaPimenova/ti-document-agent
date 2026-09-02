package com.wk.ti.common.ai.config.openai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Validated
@ConfigurationProperties(prefix = "spring.ai")
@Component
public class AIConfig {

    @Valid
    @NotNull
    private OpenAI openai;

    @Valid
    @NotNull
    private Retry retry;

    private Duration timeout;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Retry {
        private int maxAttempts = 2;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAI {
        @NotBlank
        private String apiKey;

//        @NotBlank
//        private String baseUrl;

        @Valid
        @NotNull
        private Chat chat;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chat {

        @NotBlank
        private String baseUrl;
        @NotBlank
        private String model;

        private Double temperature;
    }

}
