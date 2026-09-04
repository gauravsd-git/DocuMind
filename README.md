# DocuMind

> AI-Powered Document Q&A Engine using Retrieval-Augmented Generation (RAG)

**Live Demo:** https://docu-mind-blond.vercel.app

**Backend API:** https://documind-l1tx.onrender.com

---

## 📌 Overview

DocuMind is a full-stack AI-powered document question-answering system built using **Retrieval-Augmented Generation (RAG)**.

Users can upload PDF documents and ask natural-language questions about their content.

Instead of relying only on an LLM's existing knowledge, DocuMind retrieves relevant information from the uploaded documents and provides it to the LLM as context for generating a grounded answer with source citations.

The project was built to understand the complete engineering pipeline behind an AI-powered RAG application rather than simply integrating an LLM API.

---

# ✨ Features

## Document Processing

- PDF document upload
- PDF text extraction using Apache Tika
- Intelligent text chunking
- Chunk overlap for preserving context
- Gemini embedding generation
- 3072-dimensional embeddings
- PostgreSQL + pgvector vector storage

## RAG Question Answering

- Natural-language document questions
- Query embedding generation
- Semantic similarity search
- Top-K relevant chunk retrieval
- Context construction
- Gemini-based answer generation
- Source/citation information in responses

## Security

- User registration
- User login
- JWT-based authentication
- BCrypt password hashing
- Protected REST endpoints
- User/document ownership validation
- User-level data isolation
- CORS configuration
- Server-side file validation

## Reliability

- AI service error handling
- Resilience4j circuit breaker
- AI service fallback response
- Retry configuration
- Response caching
- SHA-256 duplicate document detection

## Developer Experience

- Swagger/OpenAPI documentation
- Postman API testing
- JUnit tests
- Docker-based local PostgreSQL + pgvector
- React frontend
- Production deployment

---

# 🧠 Why RAG?

Traditional LLM applications have an important limitation:

> An LLM does not automatically know the contents of a user's private documents.

RAG solves this problem by retrieving relevant information from external data and providing that information to the LLM as context.

For DocuMind:

```text
User Question
      ↓
Question Embedding
      ↓
Vector Search
      ↓
Relevant Document Chunks
      ↓
Context
      ↓
Gemini
      ↓
Grounded Answer
      ↓
Sources / Citations
```

This allows DocuMind to answer questions using information from uploaded documents.

---

# 🔄 RAG Pipeline

## Document Ingestion

```text
PDF Document
     ↓
Apache Tika
     ↓
Raw Text
     ↓
Text Chunking
     ↓
Document Chunks
     ↓
Gemini Embedding Model
     ↓
Embedding Vectors
     ↓
PostgreSQL + pgvector
```

## Question Answering

```text
User Question
     ↓
Question Embedding
     ↓
Query Vector
     ↓
Cosine Similarity Search
     ↓
Top-K Relevant Chunks
     ↓
Retrieved Context
     ↓
Context + Question
     ↓
Gemini LLM
     ↓
Grounded Answer
     ↓
Sources / Citations
```

---

# 🏗️ System Architecture

```text
                         USER
                           |
                           | HTTPS
                           v
              ┌────────────────────────┐
              │   React Frontend       │
              │   Vite + Tailwind CSS  │
              └───────────┬────────────┘
                          |
                          | REST API + JWT
                          v
              ┌────────────────────────┐
              │   Spring Boot Backend  │
              │                        │
              │   Spring Security      │
              │   REST Controllers     │
              │   Services             │
              │   RAG Pipeline         │
              └───────────┬────────────┘
                          |
             ┌────────────┴────────────┐
             |                         |
             v                         v
 ┌──────────────────────┐   ┌──────────────────────┐
 │ PostgreSQL           │   │ Google Gemini        │
 │ + pgvector           │   │                      │
 │                      │   │ Embeddings           │
 │ Users                │   │ Answer Generation    │
 │ Documents            │   │                      │
 │ Chunks               │   └──────────────────────┘
 │ Embeddings           │
 └──────────────────────┘
```

---

# 🌐 Production Architecture

DocuMind is deployed as a multi-service application.

```text
                         USER
                           |
                           | HTTPS
                           v
              ┌────────────────────────┐
              │ Vercel                 │
              │ React + Vite Frontend  │
              └───────────┬────────────┘
                          |
                          | HTTPS
                          | REST API + JWT
                          v
              ┌────────────────────────┐
              │ Render                 │
              │ Spring Boot Backend    │
              └───────────┬────────────┘
                          |
             ┌────────────┴────────────┐
             |                         |
             v                         v
 ┌──────────────────────┐   ┌──────────────────────┐
 │ Supabase             │   │ Google Gemini        │
 │ PostgreSQL           │   │                      │
 │ + pgvector           │   │ Embeddings           │
 │                      │   │ Generation           │
 └──────────────────────┘   └──────────────────────┘
```

### Production Services

| Service       | Responsibility                       |
| ------------- | ------------------------------------ |
| Vercel        | React frontend hosting               |
| Render        | Spring Boot backend hosting          |
| Supabase      | Managed PostgreSQL                   |
| pgvector      | Vector storage and similarity search |
| Google Gemini | Embeddings and answer generation     |
| GitHub        | Source code and deployment source    |

The browser communicates with the Spring Boot backend through HTTPS.

The browser does not directly access PostgreSQL or Gemini.

---

# 🧠 Core RAG Concepts

## Embeddings

An embedding converts text into a numerical representation that captures semantic information.

Conceptually:

```text
Text
 ↓
Embedding Model
 ↓
[0.12, -0.44, 0.81, ...]
```

DocuMind generates embeddings for both:

* Document chunks
* User questions

The resulting vectors can then be compared to find semantically relevant content.

---

## Vectors

An embedding is represented as a vector — a list of numerical values.

Conceptually:

```text
Vector =
[x1, x2, x3, ... x3072]
```

DocuMind stores these vectors using PostgreSQL + pgvector.

---

## Vector Dimensions

The dimension of a vector represents how many numerical values it contains.

The embedding model used by DocuMind produces:

```text
3072-dimensional vectors
```

Therefore the database vector representation must be compatible with the embedding model output.

---

## Cosine Similarity

Cosine similarity measures how similarly two vectors are oriented in vector space.

DocuMind uses vector similarity to determine which document chunks are most relevant to a user's question.

```text
Question Vector
       |
       v
Compare With
       |
       v
Document Chunk Vectors
       |
       v
Similarity Scores
```

Higher similarity generally indicates stronger semantic relevance.

---

## Top-K Retrieval

DocuMind does not send every document chunk to Gemini.

Instead, it retrieves the most relevant `K` chunks.

Example:

```text
Chunk A → 0.94
Chunk B → 0.89
Chunk C → 0.82
Chunk D → 0.31
```

If:

```text
K = 3
```

the system retrieves:

```text
Chunk A
Chunk B
Chunk C
```

These chunks are then used to construct the context provided to Gemini.

---

# 📄 Document Processing

When a PDF is uploaded:

```text
PDF
 ↓
Apache Tika
 ↓
Raw Text
 ↓
Chunking
 ↓
Document Chunks
 ↓
Gemini Embeddings
 ↓
PostgreSQL + pgvector
```

DocuMind uses approximately 500-word chunks with overlap.

The overlap helps preserve context between neighboring chunks.

Conceptually:

```text
Chunk 1
[----------------------]

             [----------------------]
             Chunk 2
```

The overlap reduces the possibility of losing information at chunk boundaries.

---

# ❓ Query Processing

When a user asks a question:

```text
User Question
      ↓
Generate Query Embedding
      ↓
Query Vector
      ↓
Vector Similarity Search
      ↓
Relevant Document Chunks
      ↓
Top-K Selection
      ↓
Build Context
      ↓
Context + Question
      ↓
Gemini
      ↓
Generated Answer
      ↓
Sources
```

The process can be divided into two major stages:

### Retrieval

Find information relevant to the question.

### Generation

Provide the retrieved information to Gemini so it can generate a natural-language answer.

This separation between retrieval and generation is the core idea behind RAG.

---

# 🤖 Spring AI

DocuMind uses Spring AI to integrate AI models into the Spring Boot application.

Conceptually:

```text
EmbeddingModel
      ↓
Text → Vector
```

and:

```text
ChatModel
      ↓
Prompt → Generated Answer
```

Spring AI provides abstractions that reduce direct coupling between application code and provider-specific AI APIs.

---

# 🗄️ PostgreSQL + pgvector

DocuMind uses PostgreSQL together with the pgvector extension.

This allows relational application data and vector embeddings to exist within the same database system.

Conceptually:

```text
PostgreSQL
│
├── users
│
├── documents
│
├── document_chunks
│
└── query_logs
```

Document chunks contain information such as:

```text
chunk_text
chunk_index
embedding
```

At query time:

```text
Question Vector
      ↓
pgvector
      ↓
Similarity Search
      ↓
Relevant Document Chunks
```

---

# 🔐 Security Architecture

DocuMind uses JWT-based authentication.

## Authentication Flow

```text
User
 ↓
Login
 ↓
Spring Security
 ↓
Credentials Validated
 ↓
JWT Generated
 ↓
Frontend
```

For protected requests:

```text
Frontend
   ↓
Authorization: Bearer <JWT>
   ↓
Spring Security
   ↓
JWT Validation
   ↓
Authenticated User
   ↓
Controller
```

Protected endpoints are secured by the backend.

Frontend UI restrictions alone are not considered authorization.

---

# 👤 User Data Isolation

Documents belong to users.

Conceptually:

```text
User A
 ├── Document 1
 └── Document 2

User B
 ├── Document 3
 └── Document 4
```

User A must not be able to access User B's documents.

The backend therefore performs ownership validation instead of trusting IDs supplied by the client.

---

# 🛡️ Input Validation

DocuMind performs server-side validation for document uploads.

Examples include:

* File type validation
* File size validation
* Invalid upload handling
* Duplicate document detection

Frontend validation can improve user experience, but backend validation remains authoritative because clients cannot be trusted.

---

# #️⃣ SHA-256 Duplicate Detection

DocuMind prevents duplicate document uploads using SHA-256 hashing.

```text
Uploaded PDF
     ↓
SHA-256
     ↓
Document Hash
     ↓
Compare With Existing Documents
```

If the same document is uploaded again:

```text
Same content
Different filename
      ↓
Same SHA-256 hash
      ↓
Duplicate detected
```

This means renaming a PDF does not bypass duplicate detection.

---

# ⚡ Response Caching

DocuMind includes response caching to avoid repeatedly performing the same expensive query operation when a previous result can be reused.

Conceptually:

```text
Question
   ↓
Cache Lookup
   |
   ├── Cache Hit
   │      ↓
   │   Return Cached Response
   │
   └── Cache Miss
          ↓
       RAG Pipeline
          ↓
       Gemini
          ↓
       Store Result
          ↓
       Return Response
```

Caching can reduce:

* AI API calls
* Response latency
* Repeated computation
* AI quota consumption

---

# 🧯 Resilience and Failure Handling

External AI services can fail or become temporarily unavailable.

DocuMind uses **Resilience4j Circuit Breaker** around the Gemini generation path.

```text
Application
    ↓
Circuit Breaker
    ↓
Gemini
```

If failures occur repeatedly, the circuit breaker can prevent continuous requests to an unhealthy dependency.

The backend also provides a fallback response:

```text
The AI service is temporarily unavailable.
Please try again shortly.
```

Retry behavior is configured with limited attempts and backoff.

---

# 🌐 CORS

The production frontend and backend have different origins.

```text
Frontend
https://docu-mind-blond.vercel.app

        ↓ HTTPS

Backend
https://documind-l1tx.onrender.com
```

Because browsers enforce cross-origin security rules, the backend explicitly allows the production frontend origin.

The backend CORS configuration allows:

* Production frontend origin
* Required HTTP methods
* Authorization header
* Content-Type header
* OPTIONS preflight requests

CORS is not an authentication mechanism.

Spring Security still validates JWTs and authorization rules.

---

# 🧪 Testing

DocuMind was tested at multiple levels.

## Automated Tests

JUnit tests cover areas including:

* Retrieval logic
* Embedding-related behavior
* Circuit breaker behavior
* Service functionality
* Application context

## API Testing

Postman was used for:

* Registration
* Login
* JWT-protected endpoints
* Document upload
* Document retrieval
* Document deletion
* Querying
* Invalid requests
* Authentication failures
* Authorization/ownership cases
* Duplicate document uploads

## Browser Testing

The deployed frontend was tested through the complete flow:

```text
Register
   ↓
Login
   ↓
Upload PDF
   ↓
Ask Question
   ↓
Retrieve Relevant Chunks
   ↓
Generate Answer
   ↓
Display Sources
```

---

# 📡 REST API

## Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

## Documents

```text
POST   /api/documents/upload
GET    /api/documents
DELETE /api/documents/{id}
```

## Query

```text
POST /api/query
```

Example request:

```json
{
  "question": "What does the document say about annual leave?"
}
```

Conceptual response:

```json
{
  "answer": "Employees receive ...",
  "sources": [
    {
      "documentId": 1,
      "chunkIndex": 3
    }
  ]
}
```

---

# 📚 API Documentation

Swagger/OpenAPI documentation is available in the Spring Boot backend.

Swagger provides interactive API documentation and makes it easier to inspect and test the REST API.

---

# 🖥️ Frontend

The frontend is built using:

* React
* Vite
* Tailwind CSS

The dashboard provides:

* Navigation
* Authentication
* PDF upload
* Upload status
* Question input
* AI answer display
* Source/citation display
* Logout

---

# 🏗️ Project Structure

```text
DocuMind/
│
├── documind/
│   │
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/documind/
│   │   │   │
│   │   │   └── resources/
│   │
│   ├── pom.xml
│   ├── docker-compose.yml
│   └── README.md
│
├── documind-frontend/
│   │
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.js
│   └── index.html
│
└── README.md
```

---

# 🧰 Technology Stack

| Technology        | Purpose                                |
| ----------------- | -------------------------------------- |
| Java 21           | Backend development                    |
| Spring Boot       | Backend framework and REST API         |
| Spring AI         | AI model integration                   |
| Google Gemini     | Embeddings and answer generation       |
| Apache Tika       | PDF text extraction                    |
| PostgreSQL        | Relational database                    |
| pgvector          | Vector storage and similarity search   |
| JPA / Hibernate   | ORM and persistence                    |
| Spring Security   | Authentication and authorization       |
| JWT               | Stateless authentication               |
| BCrypt            | Password hashing                       |
| Resilience4j      | Circuit breaker and resilience         |
| React             | Frontend UI                            |
| Vite              | Frontend development and build tooling |
| Tailwind CSS      | UI styling                             |
| JUnit             | Automated testing                      |
| Postman           | API testing                            |
| Swagger / OpenAPI | API documentation                      |
| Docker            | Local infrastructure                   |
| Git / GitHub      | Version control                        |
| Vercel            | Frontend deployment                    |
| Render            | Backend deployment                     |
| Supabase          | Managed PostgreSQL                     |

---

# 🐳 Local Development

## Prerequisites

Install:

* Java 21
* Maven
* Node.js
* Docker
* Git
* A Gemini API key

---

## Start PostgreSQL + pgvector

From the backend directory:

```bash
docker compose up -d
```

Check the container:

```bash
docker ps
```

---

# ⚙️ Backend Setup

Navigate to the backend:

```bash
cd documind
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

Backend:

```text
http://localhost:8080
```

---

# 🎨 Frontend Setup

Navigate to:

```bash
cd documind-frontend
```

Install dependencies:

```bash
npm install
```

Run the development server:

```bash
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

# 🔑 Environment Variables

## Backend

Configure the following environment variables:

```env
SPRING_DATASOURCE_URL=your_database_url
SPRING_DATASOURCE_USERNAME=your_database_username
SPRING_DATASOURCE_PASSWORD=your_database_password

GEMINI_API_KEY=your_gemini_api_key

JWT_SECRET=your_jwt_secret
```

## Frontend

Local development:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Production:

```env
VITE_API_BASE_URL=https://documind-l1tx.onrender.com
```

> Never commit real credentials, API keys, passwords or JWT secrets to GitHub.

> `VITE_` variables are available to browser code, so they must not contain sensitive server-side secrets.

---

# 🚀 Deployment

DocuMind is deployed as a multi-service application.

```text
GitHub
  |
  ├───────────────┐
  |               |
  v               v
Vercel          Render
Frontend        Backend
  |               |
  |               v
  |           Supabase
  |           PostgreSQL
  |             +
  |           pgvector
  |               |
  |               +
  |            Gemini
  |
  └──── HTTPS API ────>
```

## Frontend Deployment

The React/Vite frontend is deployed using Vercel.

Typical deployment process:

```text
Git Push
   ↓
Vercel detects commit
   ↓
npm install
   ↓
npm run build
   ↓
dist/
   ↓
Frontend deployed
```

---

## Backend Deployment

The Spring Boot backend is deployed using Render.

Typical deployment process:

```text
Git Push
   ↓
Render detects commit
   ↓
Build
   ↓
Start Spring Boot
   ↓
Environment Variables Injected
   ↓
Database Connection
   ↓
Backend Available
```

---

## Database Deployment

Production PostgreSQL is hosted using Supabase with pgvector.

```text
Spring Boot
     ↓
HikariCP
     ↓
JDBC
     ↓
Supabase PostgreSQL
     ↓
pgvector
```

---

# 🔒 Production Security

Sensitive configuration is stored outside the source code.

Sensitive values include:

```text
Gemini API Key
JWT Secret
Database Password
```

These values should be stored as backend deployment environment variables.

They should never be:

```text
❌ committed to GitHub
❌ embedded in React code
❌ exposed in API responses
❌ printed in logs
```

---

# 📊 Production Verification

A deployment is not complete simply because the website loads.

The complete application flow should be tested:

```text
Frontend
   ↓
Authentication
   ↓
JWT
   ↓
Document Upload
   ↓
Backend Processing
   ↓
Database
   ↓
Embedding
   ↓
Vector Search
   ↓
Gemini
   ↓
Answer
   ↓
Sources
```

Verification checklist:

* [x] Frontend loads
* [x] Registration works
* [x] Login works
* [x] JWT authentication works
* [x] PDF upload works
* [x] Backend connects to managed PostgreSQL
* [x] pgvector is available
* [x] Question submission works
* [x] RAG retrieval works
* [x] Gemini generation works
* [x] Sources are displayed
* [x] CORS works between frontend and backend
* [x] Production frontend communicates with production backend

---

# 🧩 Engineering Challenges & Lessons Learned

One of the main goals of DocuMind was to learn by actually implementing and debugging the system.

Several real engineering problems were encountered.

---

## 1. DataSource Configuration

The application initially experienced startup problems related to database configuration.

This helped establish the runtime relationship:

```text
Spring Boot
     ↓
DataSource
     ↓
HikariCP
     ↓
PostgreSQL
     ↓
JPA / Hibernate
```

---

## 2. Vector Persistence Type Mismatch

Embeddings could be generated successfully, but persistence failed when the Java/JDBC representation did not correctly match PostgreSQL's `vector` type.

This highlighted the boundary:

```text
Java
 ↓
JDBC
 ↓
Hibernate
 ↓
PostgreSQL
 ↓
pgvector
```

Generating an embedding successfully does not automatically mean it can be persisted correctly.

---

## 3. Database Status Constraint

The application encountered a mismatch between document status values represented by the Java application and the values accepted by a PostgreSQL CHECK constraint.

This demonstrated the importance of keeping:

```text
Application State
       ↕
Database Constraints
```

synchronized.

---

## 4. Local PostgreSQL Availability

Automated Spring Boot tests initially failed because the application attempted to connect to PostgreSQL while the local Docker container was not running.

The failure path was:

```text
Spring Boot Test
      ↓
ApplicationContext
      ↓
JPA / Hibernate
      ↓
PostgreSQL Connection
      ↓
Connection Refused
```

The important lesson was that the Hibernate dialect error was a secondary symptom of the unavailable database connection.

Starting the PostgreSQL container resolved the issue.

---

## 5. CORS During Production Deployment

After deploying the frontend and backend separately, browser requests initially failed because the Vercel frontend and Render backend had different origins.

The browser sent a CORS preflight request.

The backend initially did not allow the production frontend origin.

The solution was to configure Spring Security CORS handling to allow:

```text
Production frontend origin
Authorization header
Content-Type header
Required HTTP methods
OPTIONS preflight requests
```

This demonstrated an important distinction:

```text
Postman request succeeds
        ≠
Browser request automatically succeeds
```

Browsers enforce CORS policies that API clients such as Postman do not enforce in the same way.

---

# 🧪 Development Approach

The project followed an incremental learning and implementation process:

```text
Understand Concept
        ↓
Understand Why It Exists
        ↓
Understand How It Works
        ↓
Implement
        ↓
Test
        ↓
Debug
        ↓
Document
```

The goal was to understand the system instead of treating AI integration as a black box.

---

# 📈 Project Status

| Phase                                       | Status     |
| ------------------------------------------- | ---------- |
| Phase 1 — Core Backend + Document Ingestion | ✅ Complete |
| Phase 2 — Gemini + Embeddings               | ✅ Complete |
| Phase 3 — Retrieval + Generation            | ✅ Complete |
| Phase 4 — Security + Resilience             | ✅ Complete |
| Phase 5 — React Frontend                    | ✅ Complete |
| Phase 6 — Deployment + Documentation        | ✅ Complete |

---

# 🔮 Future Improvements

Possible future improvements include:

* Asynchronous document processing
* Background job/queue for large documents
* More advanced chunking strategies
* Hybrid keyword + vector search
* Vector indexes for larger datasets
* Streaming LLM responses
* Improved document processing status tracking
* More comprehensive frontend automated tests
* Observability and metrics
* Object storage for original PDFs
* Improved production scaling
* More advanced caching strategies
* Additional document formats

---

# 💡 What I Learned

The most interesting part of DocuMind has been discovering how many layers exist underneath a seemingly simple question-answering application.

```text
Document
   ↓
Text
   ↓
Chunks
   ↓
Embeddings
   ↓
Vectors
   ↓
Vector Search
   ↓
Cosine Similarity
   ↓
Top-K Retrieval
   ↓
Context
   ↓
LLM
   ↓
Answer
   ↓
Sources
```

Building the system helped move beyond simply integrating AI APIs toward understanding the engineering concepts that make AI applications work.

It also demonstrated that building an AI application involves much more than the model itself:

```text
AI Model
+
Backend
+
Database
+
Vector Search
+
Security
+
Resilience
+
Frontend
+
Networking
+
Deployment
```

---

# 👨‍💻 Author

## Gaurav Vishwakarma

DocuMind represents a practical learning journey from:

```text
Java
  ↓
Spring Boot
  ↓
REST APIs
  ↓
AI Integration
  ↓
LLMs
  ↓
Embeddings
  ↓
Vector Search
  ↓
RAG
  ↓
Security
  ↓
React
  ↓
Cloud Deployment
```

> **Learning how to use the technology, while also understanding why it works.**

---

# ⭐ Final Note

DocuMind was built as a practical exploration of how modern AI-powered applications are designed.

The goal was not simply to make an application that can answer questions.

The goal was to understand the complete engineering pipeline behind it:

```text
User
 ↓
Frontend
 ↓
REST API
 ↓
Security
 ↓
Document Processing
 ↓
Embeddings
 ↓
Vector Database
 ↓
Retrieval
 ↓
LLM
 ↓
Answer
 ↓
Citations
 ↓
Production Deployment
```

**That's DocuMind.**
