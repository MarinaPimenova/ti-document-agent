package com.wk.ti.ai.logger.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("NullableProblems")
@Slf4j
public class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

    private final ClientHttpResponse response;
    private final byte[] body;

    public BufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
        this.response = response;

        try {
            this.body = StreamUtils.copyToByteArray(response.getBody());
        } catch (IOException ex) {
            try {
                response.close();
            } catch (Exception closeEx) {
                log.warn("Failed to close response after IOException in BufferingClientHttpResponseWrapper constructor", closeEx);
            }
            throw ex;
        }
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        // Convert HttpStatusCode to int and then find the matching HttpStatus
        int statusCode = response.getStatusCode().value();
        return HttpStatus.valueOf(statusCode);
    }


    @Override
    public String getStatusText() throws IOException {
        return response.getStatusText();
    }

    @Override
    public void close() {
        response.close();
    }

    @Override
    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }
}

