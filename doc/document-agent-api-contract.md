# Document Agent API Contract

## Overview

- Repository: `ti-document-agent`
- Microservice: `document-agent`

> Note on authentication tagging: none of the controllers in this repository use the `/api/v1/...` convention. All controllers use `/rest/v1/...`, which per the documentation convention is treated as **unauthenticated**.

## DocumentAgentController

Source: `src/main/java/com/wk/ti/rag/controller/DocumentAgentController.java` — class-level mapping `@RequestMapping("/rest/v1")`.

| Action | Route or REST API | Request Payload / Response |
|---|---|---|
| Document Agent (unauthenticated)->Generate Agent Response (Async) | POST `<server address>/rest/v1/document-agent/docs?conversationId=conv-123` | Req: {"questionId": 1, "question": "What is the total revenue for 2023?"} · Resp: {"conversationId": "conv-123", "questionId": 1, "termList": "revenue, 2023", "agentType": "DOCUMENT", "sourceSet": {"headers": ["url"], "rows": [["https://example.com/analysis/123"]], "rawCount": 1}, "documentSet": {"documents": [{"title": "Annual Report 2023", "type": "pdf", "url": "https://example.com/docs/annual-report.pdf", "similarity": 0.87}]}, "summary": "Revenue for 2023 was..."} — HTTP 200 OK on success (resolved via `DeferredResult`). On internal error the same endpoint resolves with HTTP 500 and a fallback `DocumentAgentResponse` body; on timeout it resolves with HTTP 504; if the executor rejects the task it resolves with HTTP 503 (all determined from `DeferredResultService.getDeferredResult`). Query param `conversationId: String` (required, `@RequestParam`); note: the method also declares a `deferredResultTimeout` value sourced from `@Value("${agent.deferred-result-timeout:66000}")` — this is a server-side configuration value, not a client-supplied request parameter, so it is not part of the request contract. |

## TestController

Source: `src/main/java/com/wk/ti/rag/controller/TestController.java` — class-level mapping `@RequestMapping("/rest/v1/test")`.

| Action | Route or REST API | Request Payload / Response |
|---|---|---|
| Test (unauthenticated)->Generate Agent Response (Sync/Test) | POST `<server address>/rest/v1/document-agent/test/docs?conversationId=conv-123` | Req: {"questionId": 1, "question": "What is the total revenue for 2023?"} · Resp: {"conversationId": "conv-123", "questionId": 1, "termList": "revenue, 2023", "agentType": "DOCUMENT", "sourceSet": {"headers": ["url"], "rows": [["https://example.com/analysis/123"]], "rawCount": 1}, "documentSet": {"documents": [{"title": "Annual Report 2023", "type": "pdf", "url": "https://example.com/docs/annual-report.pdf", "similarity": 0.87}]}, "summary": "Revenue for 2023 was..."} — HTTP 200 OK (`ResponseEntity.ok(response)`). Query param `conversationId: String` (required, `@RequestParam`). |

## DocumentController

Source: `src/main/java/com/wk/ti/question/controller/DocumentController.java` — class-level mapping `@RequestMapping("/rest/v1")`.

| Action | Route or REST API | Request Payload / Response |
|---|---|---|
| Documents (unauthenticated)->List Documents | GET `<server address>/rest/v1/document-agent/documents` | [{"id": 1, "filename": "annual-report.pdf"}, {"id": 2, "filename": "quarterly-summary.docx"}] — HTTP 200 OK (`ResponseEntity.ok(...)`). |
| Documents->Generate Questions for Document | POST `<server address>/rest/v1/document-agent/documents/{id}/question-generation` (example: `/rest/v1/document-agent/documents/1/question-generation`) | Path variable `id: number`. Req: {"userMessage": "Generate questions about revenue trends", "requestedQuestionCount": 5} · Resp: [{"question": "What was the total revenue in 2023?", "answer": "Revenue in 2023 was...", "level": {"id": 1, "code": "EASY"}, "tags": [{"id": 1, "tag": "finance"}], "resources": ["annual-report.pdf:1:3"]}] — HTTP 200 OK (`ResponseEntity.ok(...)`). |

## VersionController

Source: `src/main/java/com/wk/ti/common/controller/VersionController.java` — no class-level `@RequestMapping`; mapping is declared directly on the method.

| Action | Route or REST API | Request Payload / Response |
|---|---|---|
| Version (unauthenticated)->Get Application Version | GET `<server address>/rest/v1/document-agent/version` | "1.0.0" — plain text response (`produces = MediaType.TEXT_PLAIN_VALUE`), returned directly as `String` from `versionService.getVersion()`. HTTP 200 OK (default for a normally-returning `@GetMapping` method; no explicit status manipulation in source). |

## Excluded from this document

- `com.wk.ti.common.controller.RestExceptionHandler` is a `@RestControllerAdvice` (global exception handler), not a `@RestController`, and defines no request-mapped endpoints — it is excluded per the task scope.
