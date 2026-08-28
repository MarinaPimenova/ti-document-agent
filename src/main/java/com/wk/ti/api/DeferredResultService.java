package com.wk.ti.api;

import com.wk.ti.api.dto.AgentPayload;

import com.wk.ti.api.dto.DocumentAgentResponse;

import com.wk.ti.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeferredResultService {
    private final RagService ragService;
    private final ExecutorService agentExecutor;

    public DeferredResult<ResponseEntity<DocumentAgentResponse>> getDeferredResult(
            String conversationId,
            AgentPayload request,
            Long deferredResultTimeout) {

        // timeout slightly larger than agent's processing expectation (ms)
        DeferredResult<ResponseEntity<DocumentAgentResponse>> output = new DeferredResult<>(deferredResultTimeout);

        // Capture security context before switching threads
        //SecurityContext securityContext = SecurityContextHolder.getContext();

        try {
            Future<?> future = agentExecutor.submit(() -> {
                long start = System.currentTimeMillis();
                // Save previous thread's context (maybe null)
                //SecurityContext originalContext = SecurityContextHolder.getContext();
                // Restore context in this thread
                //SecurityContextHolder.setContext(securityContext);
                try {
                    DocumentAgentResponse response = ragService.generate(conversationId, request);
                    output.setResult(ResponseEntity.ok(response));
                } catch (Exception ex) {
                    output.setErrorResult(ResponseEntity.status(500).body(
                            DocumentAgentResponse.fallback(
                                    conversationId,
                                    request.questionId(),
                                    "Error: " + ex.getMessage())
                    ));
                } finally {
                    // Restore the previous context, don't clear it!
                    //SecurityContextHolder.setContext(originalContext);
                    //agentContextHolder.remove();
                    log.info("SQL Agent Total elapsed time: {} ms", (System.currentTimeMillis() - start));
                }
            });
            // Optionally, you can use the Future to cancel the task on timeout
            output.onTimeout(() -> {
                // Try cancelling if still running
                future.cancel(true); // mayInterruptIfRunning = true
                try {
                    output.setErrorResult(ResponseEntity.status(504).body(
                            DocumentAgentResponse.fallback(
                                    conversationId,
                                    request.questionId(),
                                    "Request timeout while processing agent request " +
                                            "(conversationId=" + conversationId + ", questionId=" + request.questionId() +
                                            ")")
                    ));
                } catch (IllegalStateException ignored) {
                    // Result was already set by another thread; ignore
                    log.warn("Result was already set by another thread. QuestionId: {}", request.questionId());
                }
            });
        } catch (RejectedExecutionException rex) {
            // Task was not accepted (e.g., pool is saturated); fail immediately
            output.setErrorResult(ResponseEntity.status(503).body(
                    DocumentAgentResponse.fallback(
                            conversationId,
                            request.questionId(),
                            "Error: Agent execution resource is busy, please try again later")
            ));
        }

        return output;
    }

}
