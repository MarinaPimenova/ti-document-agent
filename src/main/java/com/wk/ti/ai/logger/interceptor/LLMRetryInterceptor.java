package com.wk.ti.ai.logger.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Retry logic: for POST to LLM url, retry once if not 200
 */
@SuppressWarnings("NullableProblems")
@Slf4j
public class LLMRetryInterceptor implements ClientHttpRequestInterceptor {
    private final String gptCompletionUrl;

    public LLMRetryInterceptor(String gptCompletionUrl) {
        this.gptCompletionUrl = gptCompletionUrl;
    }
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        boolean isTargetedPost = (
                request.getMethod() == HttpMethod.POST &&
                        matchesUrl(request.getURI())
        );

        ClientHttpResponse response = execution.execute(request, body);

        if (isTargetedPost && response.getStatusCode() != HttpStatus.OK) {
            // Only retry once
            response.close(); // Close the previous response before retrying
            log.info("Retry after the first attempt failed to execute: {}", gptCompletionUrl);
            response = execution.execute(request, body);
        }

        return response;
    }

    private boolean matchesUrl(URI uri) {
        return uri.getPath().contains(gptCompletionUrl);
    }
}

