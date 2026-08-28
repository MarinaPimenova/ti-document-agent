package com.wk.ti.ai.config.mistralai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai.mistralai")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MistralAiConfig {
    private String apiKey;
}
