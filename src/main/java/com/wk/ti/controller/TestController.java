package com.wk.ti.controller;

import com.wk.ti.api.dto.AgentPayload;
import com.wk.ti.api.dto.DocumentAgentResponse;
import com.wk.ti.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final RagService ragService;

    @PostMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentAgentResponse> docs(
            @RequestParam String conversationId,
            @RequestBody AgentPayload request
    ) {
        DocumentAgentResponse response = ragService.generate(conversationId, request);
        return ResponseEntity.ok(response);
    }

}
