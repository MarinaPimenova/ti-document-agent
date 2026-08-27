package com.wk.ti.ai.logger.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wk.ti.ai.logger.model.BufferingClientHttpResponseWrapper;
import com.wk.ti.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class DetailedLLMLoggingInterceptor implements ClientHttpRequestInterceptor {
    private final ObjectMapper objectMapper;

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        // ---------- REQUEST SIZE ----------
        int requestSize = body.length;

        long startTime = System.currentTimeMillis();

        log.info("=== LLM REQUEST ===");
        log.info("URL: {}", request.getURI());
        log.info("Method: {}", request.getMethod());
        log.info("Request Size: {} bytes", requestSize);

        log.info("Headers: {}", HeaderUtil.maskSensitiveHeaders(request.getHeaders()));

        // ---------- EXECUTE AND BUFFER RESPONSE ----------
        ClientHttpResponse response = execution.execute(request, body);
        ClientHttpResponse bufferedResponse = new BufferingClientHttpResponseWrapper(response);

        long duration = System.currentTimeMillis() - startTime;

        // ---------- RESPONSE SIZE ----------
        byte[] responseBytes = StreamUtils.copyToByteArray(bufferedResponse.getBody());
        int responseSize = responseBytes.length;

        // ---------- TRY PARSING TOKEN USAGE ----------
        Integer promptTokens = null, completionTokens = null, totalTokens = null;

        try {
            JsonNode json = objectMapper.readTree(responseBytes);
            if (json.has("usage")) {
                JsonNode usage = json.get("usage");
                promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
                completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : null;
            }
        } catch (Exception ex) {
            log.warn("Could not parse token usage: {}", ex.getMessage());
        }

        // ---------- LOG EVERYTHING ----------
        log.info("=== LLM RESPONSE ===");
        log.info("Status Code: {}", bufferedResponse.getStatusCode());
        log.info("Response Size: {} bytes", responseSize);
        log.info("Latency: {} ms", duration);

        if (promptTokens != null) {
            log.info("Token Usage → prompt: {}, completion: {}, total: {}",
                    promptTokens, completionTokens, totalTokens);
        } else {
            log.info("Token Usage: <not present>");
        }

        // Return the buffered response so downstream code can still read it
        return bufferedResponse;
    }
}
