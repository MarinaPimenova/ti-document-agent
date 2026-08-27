package com.wk.ti.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wk.ti.ai.logger.interceptor.DetailedLLMLoggingInterceptor;
import com.wk.ti.ai.logger.interceptor.LLMRetryInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.RequestContextFilter;

import java.util.List;

@SuppressWarnings("ConstantValue")
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    @Bean
    public RequestContextFilter requestContextFilter() {
        return new RequestContextFilter();
    }

    // Increase HTTP client read timeout to 120+ seconds
    @Bean
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory(
            @Value("${http.client.connect-timeout:30000}") int connectTimeout,
            @Value("${http.client.read-timeout:125000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout); // from property, default 30 seconds
        factory.setReadTimeout(readTimeout);       // from property, default 125 seconds
        return factory;
    }

    @Bean
    public RestTemplate llmRestTemplate(
            SimpleClientHttpRequestFactory simpleClientHttpRequestFactory,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.chat.completions-path}") String gptCompletionUrl) {
        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(simpleClientHttpRequestFactory)
        );

        restTemplate.setInterceptors(
                List.of(
                        new LLMRetryInterceptor(gptCompletionUrl),
                        new DetailedLLMLoggingInterceptor(objectMapper)
                ));

        return restTemplate;
    }

    // Create a RestClient.Builder that is configured to use your RestTemplate's factory and interceptors
    @Bean
    public RestClient.Builder llmRestClientBuilder(RestTemplate llmRestTemplate) {
        RestClient.Builder builder = RestClient.builder();
        // Set the request factory from your RestTemplate
        builder.requestFactory(llmRestTemplate.getRequestFactory());
        // Explicitly add interceptors from RestTemplate to RestClient.Builder
        if (llmRestTemplate.getInterceptors() != null) {
            for (ClientHttpRequestInterceptor interceptor : llmRestTemplate.getInterceptors()) {
                builder.requestInterceptor(interceptor);
            }
        }
        return builder;
    }
}
