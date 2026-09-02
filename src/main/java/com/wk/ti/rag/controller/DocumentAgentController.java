package com.wk.ti.rag.controller;

import com.wk.ti.rag.dto.AgentPayload;
import com.wk.ti.rag.service.DeferredResultService;

import com.wk.ti.rag.dto.DocumentAgentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@ResponseBody
@RequestMapping("/rest/v1")
@RequiredArgsConstructor
public class DocumentAgentController {

    private final DeferredResultService deferredResultService;

    @PostMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<DocumentAgentResponse>> docs(
            @RequestParam String conversationId,
            @RequestBody AgentPayload request,
            @Value("${agent.deferred-result-timeout:66000}") Long deferredResultTimeout) {
        return deferredResultService.getDeferredResult(conversationId, request, deferredResultTimeout);
    }

}
