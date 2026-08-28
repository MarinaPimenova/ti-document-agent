# How to optimize **a basic RAG pipeline** for the specific task:

> “Generate 10 interview questions with very short answers using the uploaded PDF.”

The important distinction is:

**PDF → chunks → embeddings → pgvector** is ingestion/indexing.
**User request → similarity search → retrieved chunks → LLM → structured interview questions** is the RAG generation flow.

 `QuestionAnswerAdvisor` can perform the retrieval part, but I would change the design slightly 
 so that we have explicit control over **how many chunks are retrieved, the prompt, source metadata, and the generated question format**.

## 1. Recommended architecture

For  `ti-document-agent`, implement:

```text
                    ┌─────────────────────┐
                    │      Uploaded PDF   │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ PDF Text Extraction │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Document Chunking   │
                    │ ~500-1000 tokens    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Mistral Embeddings  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ PostgreSQL/pgvector │
                    └─────────────────────┘


User:
"Generate 10 interview questions
 with very short answers"

                 │
                 ▼
        ┌──────────────────┐
        │ Question request │
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ Vector retrieval │
        │ top K chunks      │
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ Prompt + Context │
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ OpenAI LLM       │
        └────────┬─────────┘
                 │
                 ▼
        ┌────────────────────────┐
        │ 10 interview questions │
        │ + very short answers   │
        └────────────────────────┘
```

The most important change I recommend is: **don't make the LLM infer the task only from a generic RAG prompt.**

Explicitly tell it that this is an **interview-question generation task**.

---

# 2.  current `QuestionAnswerAdvisor`

 current code:

```java
QuestionAnswerAdvisor.builder(vectorStore).build();
```

is doing essentially this:

```text
user question
     ↓
similaritySearch()
     ↓
retrieve documents
     ↓
append documents to user prompt
     ↓
send augmented prompt to LLM
```

So this part is valid.

However, the default `SearchRequest` is not something I would leave implicit.

I'd configure it explicitly:

```java
this.questionAnswerAdvisor = QuestionAnswerAdvisor
        .builder(vectorStore)
        .searchRequest(SearchRequest.builder()
                .topK(8)
                .similarityThreshold(0.70)
                .build())
        .build();
```

The exact threshold should be tuned against  documents, but the important point is that **RAG retrieval parameters become part of  application configuration rather than hidden defaults**.

---

# 3. Create a dedicated interview-question prompt

 current prompt:

```java
you're assisting with questions.
Use the following context and chat history to answer the QUESTION...
```

is more appropriate for a chatbot.

For  requirement, use a dedicated prompt such as:

```text
you are an interview question generator.

 task is to generate interview questions based ONLY on the
provided document context.

Requirements:
- Generate exactly {number} interview questions.
- Each question must be answerable using the provided context.
- Each answer must be very short: 1-2 sentences.
- Do not use information that is not present in the context.
- Avoid duplicate or nearly duplicate questions.
- Cover different topics from the document when possible.
- Questions should be suitable for a technical interview.
- Do not mention the document or the retrieval process.

Context:
---------------------
{context}
---------------------

Generate the interview questions now.
```

For example:

```text
Generate exactly 10 interview questions.
```

The result should be something like:

```text
1. What is CORS?
   Answer: CORS is a browser security mechanism that controls
   which origins can access resources on another origin.

2. What is a preflight request?
   Answer: A preflight request is an OPTIONS request used by the
   browser to check whether a cross-origin request is permitted.

...
```

---

# 4. I recommend structured output

For  application, **don't return plain text from the LLM**.

Instead, define DTOs.

For example:

```java
public record InterviewQuestion(
        int number,
        String question,
        String answer
) {
}
```

and:

```java
public record InterviewQuestionResponse(
        List<InterviewQuestion> questions
) {
}
```

Then ask the LLM for structured output.

With Spring AI, the preferred approach is to use:

```java
ChatClient
    .prompt()
    .user(...)
    .call()
    .entity(InterviewQuestionResponse.class);
```

This gives we:

```json
{
  "questions": [
    {
      "number": 1,
      "question": "What is CORS?",
      "answer": "CORS controls which origins may access resources across origins."
    },
    {
      "number": 2,
      "question": "What is a preflight request?",
      "answer": "It is an OPTIONS request used to verify whether a cross-origin request is allowed."
    }
  ]
}
```

This is much easier for  `ti-orchestrator-api` and UI to consume.

---

# 5. One important problem in  current implementation

we currently have:

```java
PromptTemplate pt = new PromptTemplate(template);
Prompt p = pt.create(Map.of("question", question));
```

and then:

```java
.chatClient
    .prompt(p)
    .system(systemSpec -> systemSpec.text(systemPrompt)
            .param("question", question))
```

Then the `QuestionAnswerAdvisor` takes the **user message** and transforms it into:

```text
{query}

Context information is below...

{question_answer_context}
```

So  actual LLM prompt becomes a combination of:

```text
system prompt
+
 user prompt
+
QuestionAnswerAdvisor-generated context
```

That works, but it makes the prompt responsibility somewhat unclear.

For this particular agent, I'd simplify it.

---

# 6. Better `RagService`

I'd structure it approximately like this:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    @Value("classpath:/system-prompt-template.st")
    private Resource systemPrompt;

    private static final String NEW_SESSION_ID = "1";

    public DocumentAgentResponse generate(
            String conversationId,
            AgentPayload agentPayload) {

        Assert.notNull(conversationId, "conversationId cannot be null");
        Assert.notNull(agentPayload, "agentPayload cannot be null");
        Assert.notNull(agentPayload.question(), "question cannot be null");
        Assert.notNull(agentPayload.questionId(), "questionId cannot be null");

        if (NEW_SESSION_ID.equals(conversationId)) {
            conversationId = UUID.randomUUID().toString();
        }

        String question = agentPayload.question();

        InterviewQuestionResponse response = chatClient
                .prompt()
                .system(system -> system
                        .text(systemPrompt)
                        .param("question", question))
                .user(question)
                .advisors(
                        promptChatMemoryAdvisor(conversationId),
                        questionAnswerAdvisor()
                )
                .call()
                .entity(InterviewQuestionResponse.class);

        return DocumentAgentResponse.builder()
                .conversationId(conversationId)
                .questionId(agentPayload.questionId())
                .termList(question)
                .summary(toSummary(response))
                .sourceSet(SourceSet.fallbackSummary())
                .documentSet(DocumentSet.of(List.of()))
                .build();
    }

    private QuestionAnswerAdvisor questionAnswerAdvisor() {
        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .topK(8)
                                .similarityThreshold(0.70)
                                .build()
                )
                .build();
    }

    protected PromptChatMemoryAdvisor promptChatMemoryAdvisor(
            String conversationId) {

        return PromptChatMemoryAdvisor
                .builder(chatMemory)
                .conversationId(conversationId)
                .build();
    }

    private String toSummary(InterviewQuestionResponse response) {
        return response.questions()
                .stream()
                .map(q -> q.number() + ". "
                        + q.question()
                        + "\nAnswer: "
                        + q.answer())
                .collect(Collectors.joining("\n\n"));
    }
}
```

we'd need the corresponding imports, of course.

---

# 7. But there is an even better approach for  use case

Because  request is:

> "Generate 10 questions using the uploaded document"

I would **not use chat history for the actual document retrieval**.

This is important.

Suppose the conversation is:

```text
User:
Generate interview questions about Java.

User:
Now make them harder.

User:
Give me 10.
```

If `PromptChatMemoryAdvisor` participates in retrieval, the vector search query can become influenced by conversation history / augmented prompt.

For a document-generation agent, I'd prefer:

```text
User request
     │
     ▼
Vector search based on explicit request
     │
     ▼
Retrieved document chunks
     │
     ▼
Question-generation prompt
     │
     ▼
LLM
```

while chat memory is used only for conversational context where appropriate.

In other words:

**retrieval context and conversation memory should be conceptually separate.**

---

# 8. Even better: retrieve context explicitly

For  `Document Agent`, I actually prefer not using `QuestionAnswerAdvisor` at all.

Instead:

```java
List<Document> documents = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(question)
                .topK(8)
                .similarityThreshold(0.70)
                .build()
);
```

Then construct the prompt self.

This gives we complete control:

```java
String context = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n\n---\n\n"));
```

Then:

```java
InterviewQuestionResponse response = chatClient
        .prompt()
        .system("""
                we are a technical interview question generator.

                Generate exactly 10 interview questions based ONLY
                on the supplied document context.

                Rules:
                - Use only information from the context.
                - Do not use external knowledge.
                - Do not invent facts.
                - Questions must be answerable from the context.
                - Answers must be very short: 1-2 sentences.
                - Avoid duplicates.
                - Cover different topics when possible.

                DOCUMENT CONTEXT:
                --------------------
                %s
                --------------------
                """.formatted(context))
        .user("""
                Generate 10 interview questions with very short answers.
                """)
        .call()
        .entity(InterviewQuestionResponse.class);
```

### Why I prefer this for  agent

we can now easily:

* inspect retrieved documents;
* log them;
* return their metadata;
* filter by `documentId`;
* filter by uploaded file;
* change `topK`;
* implement reranking later;
* implement hybrid search later;
* calculate retrieval metrics;
* prevent irrelevant documents from reaching the LLM.

For a production **Document Agent**, explicit retrieval is cleaner.

---

# 9. we already have an important capability in `QuestionAnswerAdvisor`

Notice this:

```java
context.put(RETRIEVED_DOCUMENTS, documents);
```

Spring AI stores the retrieved documents in the advisor context.

That means we can potentially access:

```java
QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS
```

after the call and extract metadata.

This is useful because  PDF chunks should ideally have metadata such as:

```text
documentId
fileName
pageNumber
chunkNumber
```

For example:

```java
Document {
    text = "...",
    metadata = {
        "documentId": "123",
        "fileName": "java-interview.pdf",
        "pageNumber": 5,
        "chunkNumber": 12
    }
}
```

Then  final response can tell the UI:

```json
{
  "question": "What is dependency injection?",
  "answer": "Dependency injection provides required dependencies to an object rather than having it create them.",
  "sources": [
    {
      "fileName": "spring-interview.pdf",
      "page": 5
    }
  ]
}
```

That's much more valuable than returning only the generated text.

---

# 10.  PDF ingestion is just as important as generation

we mentioned that we already uploaded **one PDF into pgvector**.

I'd verify that the database contains something conceptually like:

```text
document
 ├── chunk 1
 ├── chunk 2
 ├── chunk 3
 ├── chunk 4
 └── ...
```

rather than one enormous vector representing the entire PDF.

A typical pipeline should be:

```java
Resource pdf
    ↓
PDF reader
    ↓
List<Document>
    ↓
TextSplitter
    ↓
chunks
    ↓
EmbeddingModel
    ↓
PgVectorStore
```

For example, Spring AI's document readers/splitters can be used for this.

The **chunk size and overlap matter enormously** to  RAG quality.

For technical documentation I'd start around:

```text
chunk size:     800-1200 tokens
overlap:        100-200 tokens
topK:            5-10
```

and tune based on actual results.

---

# 11. Mistral vs OpenAI in  dependencies

we currently have both:

```gradle
implementation 'org.springframework.ai:spring-ai-openai'
implementation 'org.springframework.ai:spring-ai-mistral-ai'
```

That's perfectly reasonable.

I'd use:

```text
Mistral
   ↓
EmbeddingModel
   ↓
pgvector
```

and:

```text
OpenAI
   ↓
ChatModel
   ↓
Interview question generation
```

So:

```text
                 ┌───────────────┐
                 │ Mistral       │
                 │ Embeddings    │
                 └───────┬───────┘
                         │
                         ▼
PDF → chunks → embeddings → pgvector
                              │
                              │ similarity search
                              ▼
                       relevant chunks
                              │
                              ▼
                     ┌────────────────┐
                     │ OpenAI Chat    │
                     │ Model          │
                     └───────┬────────┘
                             │
                             ▼
                     Interview questions
```

This is a good architecture for  current project.

---

# 12. Configuration

I'd make the RAG parameters configurable:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini

    mistralai:
      api-key: ${MISTRAL_API_KEY}
      embedding:
        options:
          model: mistral-embed

rag:
  retrieval:
    top-k: 8
    similarity-threshold: 0.70
```

Then:

```java
@ConfigurationProperties(prefix = "rag.retrieval")
public record RagRetrievalProperties(
        int topK,
        double similarityThreshold
) {
}
```

This is preferable to hardcoding:

```java
.topK(8)
.similarityThreshold(0.70)
```

---

# 13. The final flow I recommend

For  specific requirement, I'd make the Document Agent work like this:

```text
POST /document-agent/generate
        │
        │
        │ question =
        │ "Generate 10 interview questions
        │  with very short answers"
        ▼
┌──────────────────────────────┐
│ DocumentAgent                 │
└──────────────┬───────────────┘
               │
               ▼
      Extract generation intent
               │
               ▼
┌──────────────────────────────┐
│ VectorStore                   │
│ similaritySearch()            │
│ topK = 8                      │
│ threshold = 0.70              │
└──────────────┬───────────────┘
               │
               ▼
       8 relevant PDF chunks
               │
               ▼
┌──────────────────────────────┐
│ Generation Prompt             │
│                              │
│ Generate exactly 10 questions │
│ Answers 1-2 sentences        │
│ Context only                  │
└──────────────┬───────────────┘
               │
               ▼
          OpenAI Chat
               │
               ▼
┌──────────────────────────────┐
│ InterviewQuestionResponse     │
│                              │
│ 1. Question + answer         │
│ 2. Question + answer         │
│ ...                          │
│ 10. Question + answer        │
└──────────────────────────────┘
```

## One thing I would change immediately

Instead of treating the current:

```java
QuestionAnswerAdvisor
```

as the core of  Document Agent, I'd create a small explicit RAG layer:

```text
DocumentRetriever
        ↓
List<Document>
        ↓
PromptBuilder
        ↓
ChatClient
        ↓
StructuredResponse
```

with classes roughly:

```text
rag/
├── RagService.java
├── DocumentRetriever.java
├── InterviewQuestionGenerator.java
├── RagPromptBuilder.java
└── RagProperties.java
```
