package com.wk.ti.ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class AIConfig {
    @NotBlank
    private String apiKey;
    @NotBlank
    private String baseUrl;
    @NotNull
    private Chat chat;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Validated
    public static class Chat {
        @NotBlank
        private String completionsPath;
        @NotBlank
        private String model;
        @NotBlank
        private String embeddingsPath;
        @NotNull
        private Options options;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Options {
        @NotBlank
        private String model;

        private Integer maxTokens;

        private Double temperature;

        private Boolean streamUsage;
    }
}
