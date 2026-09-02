# ti-document-agent

A Document AI Agent built with Spring Boot that leverages Retrieval-Augmented Generation (RAG) and AI capabilities to answer questions based on document knowledge bases.

## 📋 Overview

`ti-document-agent` is a Spring Boot application that provides an intelligent document processing 
and question-answering system. 
It integrates with AI models (OpenAI and Mistral AI) and uses vector stores for semantic search across documents 
to deliver context-aware answers.

## ✨ Features

- **Retrieval-Augmented Generation (RAG)**: Combines document retrieval with generative AI for accurate, context-aware responses
- **Multi-AI Model Support**: Integrates with OpenAI and Mistral AI APIs
- **Vector Store Integration**: Uses pgvector with PostgreSQL for efficient semantic search
- **Multi-Database Architecture**: Separates knowledge, document, and assistant data stores
- **OAuth2 Security**: JWT-based authentication with Okta integration
- **RESTful API**: OpenAPI/Swagger documentation included
- **Observability**: Prometheus metrics, OpenTelemetry tracing with Zipkin support
- **Docker Ready**: Includes Dockerfile for containerized deployment
- **Structured Logging**: ECS JSON format logging for better log aggregation

## Technology Stack

### Core
- **Java**: 21
- **Spring Boot**: 4.1.0
- **Spring AI**: 2.0.1

### AI & Vector Stores
- **OpenAI API**: GPT-4 integration
- **Mistral AI**: Alternative LLM support
- **pgvector**: PostgreSQL vector store for embeddings

### Database
- **PostgreSQL**: Multi-database setup (knowledge_db, document_db, assistant_db)
- **Spring Data JPA**: ORM framework
- **Hibernate**: Persistence framework

### Security & Observability
- **Spring Security**: OAuth2 Resource Server with JWT
- **Micrometer**: Prometheus metrics
- **OpenTelemetry**: Distributed tracing with Zipkin

### Build & Deployment
- **Gradle**: Build automation
- **Spring Boot Actuator**: Health checks and metrics
- **Docker**: Containerization

## 📦 Dependencies

Key dependencies:
- Spring Boot Starters (Web, Security, Data JPA, Validation)
- Spring AI (OpenAI, Mistral AI, pgvector store)
- PostgreSQL JDBC Driver
- Apache Commons Lang 3
- Springdoc OpenAPI (Swagger)
- Lombok
- Micrometer (Prometheus, OpenTelemetry)

## 📁 Project Structure

```
ti-document-agent/
├── src/main/
│   ├── java/com/wk/ti/
│   │   ├── Application.java              # Spring Boot entry point
│   │   ├── assistant/                    # Assistant management module
│   │   │   ├── entity/
│   │   │   └── repository/
│   │   ├── document/                     # Document processing module
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── rag/                          # RAG implementation
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── executor/
│   │   │   └── service/
│   │   ├── question/                     # Question processing
│   │   ├── knowledge/                    # Knowledge base management
│   │   ├── user/                         # User management
│   │   ├── config/                       # Application configuration
│   │   ├── common/                       # Shared utilities
│   │   ├── exception/                    # Custom exceptions
│   │   ├── healthcheck/                  # Health check endpoints
│   │   └── util/                         # Utility classes
│   └── resources/
│       ├── application.yaml              # Application configuration
│       ├── system-prompt-template.st     # AI system prompt template
│       └── logback-spring-local.xml      # Logging configuration
├── build.gradle                          # Gradle build configuration
├── settings.gradle                       # Gradle settings
├── Dockerfile                            # Docker image definition
├── gradlew & gradlew.bat                # Gradle wrapper
└── http/                                # HTTP request examples
```

## Getting Started

### Prerequisites

- **Java 21** or later
- **PostgreSQL 12+** with pgvector extension
- **API Keys**: OpenAI and/or Mistral AI
- **Okta Domain**: For OAuth2 authentication

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/MarinaPimenova/ti-document-agent.git
   cd ti-document-agent
   ```

2. **Set up PostgreSQL databases**
   ```bash
   createdb knowledge_db
   createdb document_db
   createdb assistant_db
   ```

3. **Configure environment variables**
   ```bash
   export SERVER_PORT=8087
   export OKTA_DOMAIN=your-okta-domain
   export OPEN_AI_API_KEY=your-openai-key
   export MISTRAL_AI_API_KEY=your-mistral-key
   
   # Database connections
   export KNOWLEDGE_DB_URL=jdbc:postgresql://localhost:5432/knowledge_db
   export KNOWLEDGE_USER=knowledge_user
   export KNOWLEDGE_PASSWORD=qwerty
   
   export DOCUMENT_DB_URL=jdbc:postgresql://localhost:5433/document_db
   export DOCUMENT_USER=postgres
   export DOCUMENT_PASSWORD=postgres
   
   export ASSISTANT_DB_URL=jdbc:postgresql://localhost:5434/assistant_db
   export ASSISTANT_USER=assistant_user
   export ASSISTANT_PASSWORD=qwerty
   ```

4. **Build the application**
   ```bash
   ./gradlew build
   ```

5. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

   Or using the built JAR:
   ```bash
   java -jar build/libs/ti-document-agent-0.0.1-SNAPSHOT.jar
   ```

## 🐳 Docker Deployment

1. **Build Docker image**
   ```bash
   ./gradlew build
   docker build -t ti-document-agent:latest .
   ```

2. **Run container**
   ```bash
   docker run -p 8087:8087 \
     -e OKTA_DOMAIN=your-domain \
     -e OPEN_AI_API_KEY=your-key \
     -e KNOWLEDGE_DB_URL=jdbc:postgresql://host.docker.internal:5432/knowledge_db \
     ti-document-agent:latest
   ```

## API Endpoints

The application exposes a RESTful API with OpenAPI documentation:

- **API Documentation**: `http://localhost:8087/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8087/v3/api-docs`
- **Health Check**: `http://localhost:8087/actuator/health`
- **Metrics**: `http://localhost:8087/actuator/metrics`
- **Prometheus**: `http://localhost:8087/actuator/prometheus`

### Core Modules

- **RAG Module**: Question answering with document retrieval and generation
- **Document Module**: Document management and processing
- **Assistant Module**: Assistant configuration and state management
- **Question Module**: Question processing and routing
- **Knowledge Module**: Knowledge base management
- **User Module**: User management and authorization

## 📊 Observability

### Metrics (Prometheus)
Access metrics at `http://localhost:8087/actuator/prometheus`

### Tracing (Zipkin)
Configured to export traces to Zipkin at `http://localhost:9411`

### Logging
Structured JSON logging in ECS format for better log aggregation and analysis

## 🔐 Security

- **OAuth2 JWT**: Okta integration for user authentication
- **Resource Server**: Validates JWT tokens from Okta
- **Session Timeout**: 1 hour default timeout

## Testing

Run tests with:
```bash
./gradlew test
```


