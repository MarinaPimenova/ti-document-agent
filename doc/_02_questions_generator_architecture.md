# **explicit RAG pipeline** 

One important correction first: for PDF ingestion,  
current `build.gradle` is missing Spring AI's PDF reader dependency. 
Spring AI provides `PagePdfDocumentReader`, 
and its documented ETL flow is PDF reader → text splitter → vector store. ([Home][1])

Also, Spring AI 1.1.5 supports the PgVector configuration and Mistral embedding model we need. ([Home][2])

Below is a complete implementation I would use as the baseline.

---

# 1. Target structure

I recommend this structure:

```text
com.wk.ti
├── config
│   ├── AiConfig.java
│   └── RagProperties.java
│
├── rag
│   ├── controller
│   │   └── DocumentAgentController.java
│   │
│   ├── dto
│   │   ├── GenerateInterviewRequest.java
│   │   ├── InterviewQuestion.java
│   │   └── InterviewQuestionResponse.java
│   │
│   ├── service
│   │   ├── RagService.java
│   │   ├── DocumentRetriever.java
│   │   ├── InterviewQuestionGenerator.java
│   │   └── DocumentIngestionService.java
│   │
│   └── model
│       └── RetrievedDocument.java
```

The responsibilities are deliberately separated:

```text
DocumentIngestionService
        ↓
      pgvector

DocumentRetriever
        ↓
 List<Document>

InterviewQuestionGenerator
        ↓
InterviewQuestionResponse

RagService
        ↓
 orchestrates everything
```

---

# 2. Gradle changes

Keep  existing dependencies and add the PDF reader:

```gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'

    // Spring AI
    implementation 'org.springframework.ai:spring-ai-openai'
    implementation 'org.springframework.ai:spring-ai-rag'
    implementation 'org.springframework.ai:spring-ai-advisors-vector-store'
    implementation 'org.springframework.ai:spring-ai-pgvector-store'
    implementation 'org.springframework.ai:spring-ai-mistral-ai'

    // PDF ingestion
    implementation 'org.springframework.ai:spring-ai-pdf-document-reader'

    // ...
}
```

The PDF reader artifact is the Spring AI module intended for `PagePdfDocumentReader`. ([Home][1])

we can actually remove:

```gradle
implementation 'org.springframework.ai:spring-ai-advisors-vector-store'
```

if we completely move away from `QuestionAnswerAdvisor`.

we can also remove:

```gradle
implementation 'org.springframework.ai:spring-ai-rag'
```

if we don't use any other classes from that module. The explicit implementation below only requires the vector store and core AI APIs.

I would nevertheless keep them for now because  project already has them and we may use Spring AI RAG components later.

---

# 3. application.yaml

I'd configure it like this:

```yaml
spring:
  application:
    name: ti-document-agent

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/ti}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:gpt-4o-mini}
          temperature: ${OPENAI_TEMPERATURE:0.2}

    mistralai:
      api-key: ${MISTRAL_API_KEY}
      embedding:
        model: ${MISTRAL_EMBEDDING_MODEL:mistral-embed}

    vectorstore:
      pgvector:
        initialize-schema: false
        schema-name: ${PGVECTOR_SCHEMA:public}
        table-name: ${PGVECTOR_TABLE:vector_store}
        dimensions: ${PGVECTOR_DIMENSIONS:1024}
        distance-type: COSINE_DISTANCE
        index-type: HNSW
        max-document-batch-size: 1000

rag:
  retrieval:
    top-k: 8
    similarity-threshold: 0.70

  chunking:
    chunk-size: 800
    min-chunk-size-chars: 350
    min-chunk-length-to-embed: 10
    max-num-chunks: 10000

  generation:
    default-question-count: 10
    max-question-count: 20
    max-answer-sentences: 2

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

### Important: verify  embedding dimension

Do **not blindly use `1024`** if  existing PGVector table was created with another dimension.

The vector column dimension must match the embedding model. Spring AI explicitly requires the PgVector dimension to correspond to the embedding model; changing dimensions requires recreating the vector table. ([Home][3])

Check PostgreSQL:

```sql
SELECT
    atttypmod
FROM pg_attribute
WHERE attrelid = 'public.vector_store'::regclass
  AND attname = 'embedding';
```

Or simply inspect:

```sql
\d public.vector_store
```

If  existing table is, for example:

```text
embedding vector(1024)
```

keep:

```yaml
dimensions: 1024
```

If it's `vector(1536)`, use `1536`.

---

# 4. PostgreSQL

Make sure pgvector is enabled.

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

Spring AI's PgVector implementation requires these extensions for its standard schema. ([Home][3])

For production, I recommend managing these through Liquibase rather than:

```yaml
initialize-schema: true
```

So leave:

```yaml
initialize-schema: false
```

and manage  DB schema explicitly.

---

# 5. RagProperties

```java
package com.wk.ti.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        Retrieval retrieval,
        Chunking chunking,
        Generation generation
) {

    public record Retrieval(
            int topK,
            double similarityThreshold
    ) {
    }

    public record Chunking(
            int chunkSize,
            int minChunkSizeChars,
            int minChunkLengthToEmbed,
            int maxNumChunks
    ) {
    }

    public record Generation(
            int defaultQuestionCount,
            int maxQuestionCount,
            int maxAnswerSentences
    ) {
    }
}
```

---

# 6. AiConfig

Create a dedicated `ChatClient`.

```java
package com.wk.ti.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    VectorStore vectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .build();
    }
}
```

The PgVector store integrates directly with an `EmbeddingModel`; Spring AI's documented setup supports `JdbcTemplate + EmbeddingModel + PgVectorStore`. ([Home][3])

Because we're using Mistral embeddings, Spring AI can auto-configure the Mistral `EmbeddingModel`. ([Home][4])

---

# 7. Request DTO

```java
package com.wk.ti.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateInterviewRequest(

        String question,

        @Min(1)
        @Max(20)
        Integer questionCount

) {

    public int questionCountOrDefault(int defaultValue) {
        return questionCount == null ? defaultValue : questionCount;
    }
}
```

For example:

```json
{
  "question": "Generate interview questions about the uploaded document",
  "questionCount": 10
}
```

---

# 8. InterviewQuestion

```java
package com.wk.ti.rag.dto;

public record InterviewQuestion(
        int number,
        String question,
        String answer
) {
}
```

---

# 9. InterviewQuestionResponse

```java
package com.wk.ti.rag.dto;

import java.util.List;

public record InterviewQuestionResponse(
        List<InterviewQuestion> questions
) {
}
```

This is intentionally an object containing a list rather than a top-level array.

That's a good choice for provider structured output because OpenAI's native structured output has limitations around top-level arrays. Spring AI documents this explicitly. ([Home][5])

---

# 10. RetrievedDocument

I recommend keeping retrieval information separate from the Spring AI `Document`.

```java
package com.wk.ti.rag.model;

public record RetrievedDocument(
        String id,
        String content,
        double score,
        String fileName,
        Integer pageNumber
) {
}
```

However, there is an important caveat:

`Document` doesn't necessarily expose a similarity score in a portable way, depending on the vector-store implementation/version.

So don't depend on `score` unless we have verified  particular Spring AI 1.1.5 PgVector implementation exposes it through metadata.

---

# 11. DocumentRetriever

This is the heart of the retrieval part.

```java
package com.wk.ti.rag.service;

import com.wk.ti.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRetriever {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public List<Document> retrieve(String query) {

        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        RagProperties.Retrieval properties = ragProperties.retrieval();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);

        log.info(
                "RAG retrieval completed. query='{}', requestedTopK={}, retrieved={}",
                query,
                properties.topK(),
                documents.size()
        );

        return documents;
    }
}
```

Spring AI's PgVector implementation supports both `topK` and similarity thresholds, as well as metadata filtering. ([Home][3])

---

# 12. Prompt builder

Create a dedicated component.

```java
package com.wk.ti.rag.service;

import com.wk.ti.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final RagProperties ragProperties;

    public String buildSystemPrompt(
            List<Document> documents,
            int questionCount) {

        String context = buildContext(documents);

        return """
                we are a technical interview question generator.

                 task is to generate exactly %d interview questions
                using ONLY the supplied document context.

                STRICT RULES:

                1. Use only information explicitly present in the context.
                2. Do not use external knowledge.
                3. Do not invent facts.
                4. Every question must be answerable from the context.
                5. Generate exactly %d questions.
                6. Answers must be very short, no more than %d sentences.
                7. Avoid duplicate or nearly duplicate questions.
                8. Prefer questions covering different topics from the document.
                9. Questions should be appropriate for a technical interview.
                10. If the context does not contain enough information for %d
                    meaningful questions, generate only questions that can be
                    supported by the context.
                11. Do not mention the context, PDF, retrieval, or these rules
                    in the generated questions or answers.

                DOCUMENT CONTEXT
                =================
                %s
                =================
                """
                .formatted(
                        questionCount,
                        questionCount,
                        ragProperties.generation().maxAnswerSentences(),
                        questionCount,
                        context
                );
    }

    private String buildContext(List<Document> documents) {

        return IntStream.range(0, documents.size())
                .mapToObj(i -> {
                    Document document = documents.get(i);

                    return """
                            [DOCUMENT CHUNK %d]
                            %s
                            """
                            .formatted(
                                    i + 1,
                                    document.getText()
                            );
                })
                .reduce(
                        "",
                        (left, right) -> left + "\n" + right
                );
    }
}
```

---

# 13. InterviewQuestionGenerator

This is where OpenAI generates the final answer.

```java
package com.wk.ti.rag.service;

import com.wk.ti.rag.dto.InterviewQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionGenerator {

    private final ChatClient chatClient;
    private final RagPromptBuilder promptBuilder;

    public InterviewQuestionResponse generate(
            String userRequest,
            List<Document> documents,
            int questionCount) {

        if (documents.isEmpty()) {
            throw new RagNoContextException(
                    "No relevant document context was found for the request."
            );
        }

        String systemPrompt = promptBuilder.buildSystemPrompt(
                documents,
                questionCount
        );

        log.debug(
                "Generating interview questions. questionCount={}, documents={}",
                questionCount,
                documents.size()
        );

        InterviewQuestionResponse response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userRequest)
                .call()
                .entity(InterviewQuestionResponse.class);

        validate(response, questionCount);

        return response;
    }

    private void validate(
            InterviewQuestionResponse response,
            int expectedQuestionCount) {

        if (response == null || response.questions() == null) {
            throw new RagGenerationException(
                    "LLM returned an empty interview question response."
            );
        }

        if (response.questions().size() != expectedQuestionCount) {
            throw new RagGenerationException(
                    "Expected %d interview questions but received %d."
                            .formatted(
                                    expectedQuestionCount,
                                    response.questions().size()
                            )
            );
        }

        response.questions().forEach(question -> {

            if (question.question() == null
                    || question.question().isBlank()) {

                throw new RagGenerationException(
                        "Generated question cannot be empty."
                );
            }

            if (question.answer() == null
                    || question.answer().isBlank()) {

                throw new RagGenerationException(
                        "Generated answer cannot be empty."
                );
            }
        });
    }
}
```

---

# 14. Exceptions

```java
package com.wk.ti.rag.service;

public class RagNoContextException extends RuntimeException {

    public RagNoContextException(String message) {
        super(message);
    }
}
```

and:

```java
package com.wk.ti.rag.service;

public class RagGenerationException extends RuntimeException {

    public RagGenerationException(String message) {
        super(message);
    }
}
```

---

# 15. The main RagService

Now the orchestration becomes very simple.

```java
package com.wk.ti.rag.service;

import com.wk.ti.config.RagProperties;
import com.wk.ti.rag.dto.GenerateInterviewRequest;
import com.wk.ti.rag.dto.InterviewQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentRetriever documentRetriever;
    private final InterviewQuestionGenerator questionGenerator;
    private final RagProperties ragProperties;

    public InterviewQuestionResponse generate(
            GenerateInterviewRequest request) {

        Assert.notNull(request, "request cannot be null");

        String userQuestion = request.question();

        Assert.hasText(
                userQuestion,
                "question cannot be null or empty"
        );

        int questionCount = request.questionCountOrDefault(
                ragProperties.generation().defaultQuestionCount()
        );

        validateQuestionCount(questionCount);

        log.info(
                "Starting RAG generation. question='{}', questionCount={}",
                userQuestion,
                questionCount
        );

        List<Document> documents =
                documentRetriever.retrieve(userQuestion);

        if (documents.isEmpty()) {
            throw new RagNoContextException(
                    "No relevant information was found in the uploaded documents."
            );
        }

        return questionGenerator.generate(
                userQuestion,
                documents,
                questionCount
        );
    }

    private void validateQuestionCount(int questionCount) {

        if (questionCount < 1
                || questionCount > ragProperties.generation().maxQuestionCount()) {

            throw new IllegalArgumentException(
                    "Question count must be between 1 and "
                            + ragProperties.generation().maxQuestionCount()
            );
        }
    }
}
```

---

# 16. Controller

```java
package com.wk.ti.rag.controller;

import com.wk.ti.rag.dto.GenerateInterviewRequest;
import com.wk.ti.rag.dto.InterviewQuestionResponse;
import com.wk.ti.rag.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/document-agent")
@RequiredArgsConstructor
public class DocumentAgentController {

    private final RagService ragService;

    @PostMapping("/interview-questions")
    public ResponseEntity<InterviewQuestionResponse> generateInterviewQuestions(
            @Valid @RequestBody GenerateInterviewRequest request) {

        return ResponseEntity.ok(
                ragService.generate(request)
        );
    }
}
```

---

# 17.  requested call

Now we can call:

```http
POST /api/v1/document-agent/interview-questions
Content-Type: application/json
```

with:

```json
{
  "question": "Generate 10 interview questions with very short answers using the uploaded document.",
  "questionCount": 10
}
```

and receive:

```json
{
  "questions": [
    {
      "number": 1,
      "question": "What is ...?",
      "answer": "..."
    },
    {
      "number": 2,
      "question": "What is ...?",
      "answer": "..."
    }
  ]
}
```

---

# 18. PDF ingestion

This is the part that creates the vectors.

Create:

```java
package com.wk.ti.rag.service;

import com.wk.ti.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public String ingest(
            MultipartFile file,
            String documentId) {

        validate(file);

        try (InputStream inputStream = file.getInputStream()) {

            InputStreamResource resource =
                    new InputStreamResource(inputStream) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };

            PagePdfDocumentReader reader =
                    new PagePdfDocumentReader(resource);

            List<Document> pages = reader.read();

            log.info(
                    "PDF parsed. documentId={}, fileName={}, pages={}",
                    documentId,
                    file.getOriginalFilename(),
                    pages.size()
            );

            enrichMetadata(
                    pages,
                    documentId,
                    file.getOriginalFilename()
            );

            TokenTextSplitter splitter =
                    new TokenTextSplitter(
                            ragProperties.chunking().chunkSize(),
                            ragProperties.chunking().minChunkSizeChars(),
                            ragProperties.chunking().minChunkLengthToEmbed(),
                            ragProperties.chunking().maxNumChunks(),
                            true
                    );

            List<Document> chunks = splitter.split(pages);

            log.info(
                    "PDF split into chunks. documentId={}, chunks={}",
                    documentId,
                    chunks.size()
            );

            vectorStore.add(chunks);

            log.info(
                    "PDF indexed successfully. documentId={}, chunks={}",
                    documentId,
                    chunks.size()
            );

            return documentId;

        }
        catch (IOException e) {
            throw new DocumentIngestionException(
                    "Failed to read PDF: "
                            + file.getOriginalFilename(),
                    e
            );
        }
    }

    private void enrichMetadata(
            List<Document> documents,
            String documentId,
            String fileName) {

        documents.forEach(document -> {

            document.getMetadata().put(
                    "documentId",
                    documentId
            );

            document.getMetadata().put(
                    "fileName",
                    fileName
            );
        });
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new DocumentIngestionException(
                    "Uploaded file is empty."
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || !filename.toLowerCase().endsWith(".pdf")) {

            throw new DocumentIngestionException(
                    "Only PDF files are supported."
            );
        }
    }
}
```

And:

```java
package com.wk.ti.rag.service;

public class DocumentIngestionException extends RuntimeException {

    public DocumentIngestionException(String message) {
        super(message);
    }

    public DocumentIngestionException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
```

---

# 19. I would improve the ingestion metadata

This is particularly important for  architecture.

Instead of only:

```java
document.getMetadata().put("documentId", documentId);
document.getMetadata().put("fileName", fileName);
```

I'd eventually use:

```text
documentId
fileId
fileName
pageNumber
chunkNumber
contentType
uploadedBy
uploadedAt
```

For example:

```java
document.getMetadata().put(
        "documentId",
        documentId
);

document.getMetadata().put(
        "fileName",
        fileName
);

document.getMetadata().put(
        "contentType",
        "application/pdf"
);
```

This will allow we to implement:

```text
Retrieve only chunks belonging to document X
```

using PgVector metadata filtering. Spring AI's PgVector implementation supports metadata filter expressions. ([Home][3])

---

# 20. Add document filtering

This becomes especially important once we have more than one PDF.

For example:

```text
document A → Java interview.pdf
document B → Spring interview.pdf
document C → AWS interview.pdf
```

If the user says:

```text
Generate questions using Spring interview.pdf
```

we don't want:

```text
Java + Spring + AWS
```

chunks mixed together.

 retriever can support:

```java
public List<Document> retrieve(
        String query,
        String documentId) {

    SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(ragProperties.retrieval().topK())
            .similarityThreshold(
                    ragProperties.retrieval().similarityThreshold()
            )
            .filterExpression(
                    "documentId == '%s'".formatted(documentId)
            )
            .build();

    return vectorStore.similaritySearch(request);
}
```

This is one of the reasons I prefer explicit retrieval over `QuestionAnswerAdvisor`.

---

# 21. Important issue with  existing PDF

we said:

> "I uploaded one pdf document into PGVector store."

If the document is already there, **do not ingest it again**.

 immediate test should be:

```text
existing PDF
     ↓
pgvector
     ↓
DocumentRetriever
     ↓
8 chunks
     ↓
OpenAI
     ↓
10 questions
```

we can test the retrieval independently before involving the LLM.

For example:

```java
List<Document> documents =
        documentRetriever.retrieve(
                "Generate interview questions about the main technical concepts"
        );

documents.forEach(document ->
        log.info(
                "Retrieved chunk: {}",
                document.getText()
        )
);
```

This is extremely important when debugging RAG.

---

# 22. Don't debug RAG and LLM simultaneously

Use this debugging sequence.

### Step 1 — Check PostgreSQL

```sql
SELECT count(*)
FROM vector_store;
```

we should have **multiple rows**, not one row for the entire PDF.

Then:

```sql
SELECT
    id,
    left(content, 200),
    metadata
FROM vector_store
LIMIT 10;
```

we should see chunks.

---

### Step 2 — Test retrieval

Ask:

```text
What is the main topic of this document?
```

Then log:

```text
Retrieved 8 documents
```

and inspect their contents.

If the chunks are irrelevant, **don't touch the LLM prompt yet**.

Fix retrieval first.

---

### Step 3 — Test generation

Once retrieval is good:

```text
Generate 10 interview questions.
```

Then inspect the LLM output.

---

# 23. Structured output

For  use case, I strongly recommend:

```java
.entity(InterviewQuestionResponse.class)
```

rather than:

```java
.content()
```

Spring AI's `ChatClient` supports mapping responses directly to Java types through `.entity(...)`. ([Home][6])

For maximum reliability, we can additionally use provider-native structured output where supported:

```java
InterviewQuestionResponse response = chatClient
        .prompt()
        .system(systemPrompt)
        .user(userRequest)
        .call()
        .entity(
                InterviewQuestionResponse.class,
                spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema()
        );
```

Spring AI documents both `useProviderStructuredOutput()` and `validateSchema()` for this purpose. ([Home][5])

**However:** because we're specifically on Spring AI **1.1.5**, I would first compile/test the simpler:

```java
.entity(InterviewQuestionResponse.class)
```

path against  exact dependency set. If the provider-native API is available in  resolved 1.1.5 API, then add the two reliability options.

---

# 24. I would also change  current system prompt

 existing:

```text
we're assisting with questions.

Use the following context and chat history to answer the QUESTION
but act as if we knew this information innately.
```

isn't ideal for RAG.

I'd replace it with:

```text
we are a technical interview question generator.

we answer exclusively from the retrieved document context.

Never use  general knowledge to fill missing information.

If the retrieved context does not contain enough information to
support a question or answer, do not invent the information.

Generate concise, technically accurate interview questions.

Answers must be very short.
```

The phrase:

> "act as if we knew this information innately"

is something I'd remove.

It can encourage the model to treat retrieved information as its own knowledge and makes hallucination control less explicit.

---

# 25. One more architectural improvement

For  existing architecture:

```text
ti-knowledge-ui
        ↓
ti-gateway-api
        ↓
ti-orchestrator-api
        ↓
ti-document-agent
```

I'd keep the Document Agent API relatively generic:

```http
POST /api/v1/document-agent/generate
```

rather than making the API permanently specific to:

```http
/interview-questions
```

because  Document Agent may later need:

```text
generate interview questions
summarize document
answer question
extract key concepts
generate flashcards
generate quiz
compare documents
```

So I'd make the domain request:

```java
public record DocumentAgentRequest(
        String documentId,
        String instruction,
        Integer requestedItemCount
) {
}
```

Then:

```json
{
  "documentId": "8b2f...",
  "instruction": "Generate interview questions with very short answers",
  "requestedItemCount": 10
}
```

 current use case becomes just one generation strategy.

---

# 26. Final production architecture

With the changes above,  service becomes:

```text
                    ┌─────────────────────┐
                    │       PDF upload    │
                    └──────────┬──────────┘
                               │
                               ▼
                  DocumentIngestionService
                               │
                     PagePdfDocumentReader
                               │
                               ▼
                       TokenTextSplitter
                               │
                               ▼
                        Mistral Embedding
                               │
                               ▼
                         PostgreSQL
                           pgvector
                               │
                               │
        ───────────────────────┼────────────────────────
                               │
                               ▼
                       DocumentRetriever
                               │
                               │ topK=8
                               │ threshold=0.70
                               ▼
                       relevant chunks
                               │
                               ▼
                      RagPromptBuilder
                               │
                               ▼
                  InterviewQuestionGenerator
                               │
                               ▼
                         OpenAI Chat
                               │
                               ▼
                 InterviewQuestionResponse
                               │
                               ▼
                         Orchestrator
                               │
                               ▼
                              UI
```

This is a much cleaner production baseline than  current:

```text
ChatClient
  +
PromptChatMemoryAdvisor
  +
QuestionAnswerAdvisor
  +
SimpleLoggerAdvisor
```

because retrieval, prompting, generation and ingestion are now independently testable.

### One particularly important point

Since we **already have a PDF in pgvector**, I would **not change the existing vector data until we verify its embedding dimension and metadata/chunk structure**. 
The PgVector schema's vector dimension must match the embedding model, 
and Spring AI's documentation specifically notes 
that changing dimensions requires recreating the vector table. ([Home][3])

Also, Spring AI's recommended ETL pattern is exactly the one we're using here: PDF `DocumentReader` → `TokenTextSplitter` → `VectorStore`. ([Home][1])

So the next practical step is to check  existing `vector_store` table and make the implementation fit ** already-indexed PDF**, rather than re-indexing it unnecessarily.

[1]: https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html?utm_source=chatgpt.com "ETL Pipeline :: Spring AI Reference"
[2]: https://docs.spring.io/spring-ai/docs/1.1.5/api/org/springframework/ai/vectorstore/pgvector/autoconfigure/class-use/PgVectorStoreProperties.html?utm_source=chatgpt.com "Uses of Class org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties (Spring AI Parent 1.1.5 API)"
[3]: https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html?utm_source=chatgpt.com "PGvector :: Spring AI Reference"
[4]: https://docs.spring.io/spring-ai/reference/api/embeddings/mistralai-embeddings.html?utm_source=chatgpt.com "Mistral AI Embeddings :: Spring AI Reference"
[5]: https://docs.spring.io/spring-ai/reference/api/structured-output/native.html?utm_source=chatgpt.com "Provider-Native Structured Output :: Spring AI Reference"
[6]: https://docs.spring.io/spring-ai/reference/api/chatclient.html?utm_source=chatgpt.com "Chat Client API :: Spring AI Reference"
