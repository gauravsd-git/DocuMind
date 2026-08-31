# DocuMind

> **AI-Powered Document Q&A Engine**

DocuMind is an AI-powered document question-answering system built using **Retrieval-Augmented Generation (RAG)**.

It allows users to upload PDF documents and ask natural-language questions about their content. Instead of relying only on an LLM's existing knowledge, DocuMind retrieves relevant information from the uploaded documents and provides it to the LLM as context for generating a grounded answer.

---

## Why DocuMind?

While learning about AI systems and LLM integration with Spring Boot, I wanted to understand what happens beyond simply sending a prompt to an LLM.

DocuMind is my attempt to learn and build the complete RAG pipeline while combining my backend development experience with AI technologies.

The focus is not only on using AI APIs, but on understanding the concepts underneath them:

- Document processing
- Text chunking
- Embeddings
- Vector representations
- Vector dimensions
- Cosine similarity
- Semantic search
- Top-K retrieval
- Context construction
- LLM-based generation
- Source/citation grounding

---

## RAG Pipeline

```text
                    DOCUMENT INGESTION

PDF Document
     ↓
Apache Tika
(Text Extraction)
     ↓
Chunking
     ↓
Embedding Model
(Gemini)
     ↓
PostgreSQL + pgvector
(Vector Storage)


                    QUESTION ANSWERING

User Question
     ↓
Question Embedding
     ↓
Cosine Similarity Search
     ↓
Top-K Relevant Chunks
     ↓
Retrieved Context
     ↓
Context + Question
     ↓
LLM (Gemini)
     ↓
Grounded Answer
```

### In simple terms

The document is first converted into smaller chunks.

Each chunk is converted into an **embedding**, which is a numerical representation of its meaning.

These vectors are stored in PostgreSQL using **pgvector**.

When a user asks a question, the question is also converted into a vector. DocuMind compares this vector with the stored document vectors using **cosine similarity** and retrieves the most relevant chunks.

Those chunks are then provided to the LLM as context so it can generate an answer based on the uploaded document.

---

## Core RAG Concepts

### Embeddings

An embedding converts text into a numerical representation.

For example:

```text
"Employees receive 20 days of annual leave."
                    ↓
             Embedding Model
                    ↓
        [0.12, -0.44, 0.81, ...]
```

The embedding represents the semantic characteristics of the text in numerical form.

### Vectors

An embedding is represented as a vector — essentially a list of numerical values.

DocuMind uses these vectors to perform semantic similarity searches.

### Vector Dimensions

The dimension of a vector is the number of numerical values it contains.

The embedding model currently used by DocuMind produces **3072-dimensional embeddings**.

Conceptually:

```text
Vector =
[x1, x2, x3, ... x3072]

Dimension = 3072
```

The vector dimension is important because the vector representation stored in the database must be compatible with the embedding model's output.

### Cosine Similarity

Cosine similarity measures how similarly two vectors are oriented in vector space.

In DocuMind, it is used to determine how semantically relevant a document chunk is to a user's question.

Conceptually:

```text
Question Vector
       ↓
Compare with
       ↓
Document Chunk Vectors
       ↓
Similarity Score
```

A higher similarity score generally indicates that the two pieces of text are more semantically related.

### Top-K Retrieval

DocuMind does not need to send every document chunk to the LLM.

Instead, it retrieves the most relevant `K` chunks.

For example:

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

These chunks become the context used by the LLM.

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot | Application framework |
| Spring AI | LLM and embedding integration |
| Google Gemini | Embeddings and answer generation |
| Apache Tika | PDF text extraction |
| PostgreSQL | Relational database |
| pgvector | Vector storage and similarity search |
| JPA / Hibernate | ORM and data persistence |
| Spring Security + JWT | Authentication and authorization |
| JUnit | Automated testing |
| Postman | API testing |
| Swagger UI | API documentation |
| Docker | Local infrastructure |
| Git / GitHub | Version control |

---

## System Architecture

```text
                         DOCUMIND

 ┌───────────────────────────────────────────────┐
 │                Document Upload                │
 └───────────────────────┬───────────────────────┘
                         │
                         ▼
                  Apache Tika
                         │
                         ▼
                    Text Chunks
                         │
                         ▼
                  Gemini Embeddings
                         │
                         ▼
              PostgreSQL + pgvector
                         │
                         │
                         │
 ┌───────────────────────┴───────────────────────┐
 │                 Query Pipeline                │
 └───────────────────────────────────────────────┘
                         ▲
                         │
                  User Question
                         │
                         ▼
                Question Embedding
                         │
                         ▼
              Cosine Similarity Search
                         │
                         ▼
                 Top-K Chunks
                         │
                         ▼
              Retrieved Context
                         │
                         ▼
                    Gemini LLM
                         │
                         ▼
                  Final Answer
```

---

## Document Processing

When a PDF is uploaded, DocuMind processes it through the following flow:

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
Embeddings
 ↓
Vector Storage
```

Chunking is important because a large document should not be treated as one enormous piece of text.

The current design uses approximately **500-word chunks with slight overlap**.

The overlap helps preserve context between neighboring chunks.

---

## Query Processing

When a user asks a question:

```text
User Question
      ↓
Generate Embedding
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
Send Context + Question to LLM
      ↓
Generate Answer
```

This separation between **retrieval** and **generation** is the core idea behind RAG.

---

## Spring AI

DocuMind uses Spring AI to provide abstractions around AI model integration.

Two important abstractions are:

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

This allows the application to interact with AI models through Spring AI abstractions rather than coupling every part of the application directly to provider-specific APIs.

---

## Vector Storage with PostgreSQL

DocuMind uses PostgreSQL together with the **pgvector** extension.

This allows relational application data and vector embeddings to be stored within the same database system.

Conceptually:

```text
PostgreSQL
│
├── users
│
├── documents
│
└── document_chunks
      │
      ├── chunk_text
      ├── chunk_index
      └── embedding
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

## Data Model

### User

```text
id
email
password
role
```

### Document

```text
id
userId
filename
uploadDate
status
```

### DocumentChunk

```text
id
documentId
chunkText
chunkIndex
embeddingVector
```

### QueryLog

```text
id
userId
question
answer
sourceChunks
timestamp
```

Relationship:

```text
User
 │
 └── Documents
       │
       └── DocumentChunks
```

---

## REST API

The core API provides endpoints for authentication, document management, and querying.

```text
POST   /api/auth/register
POST   /api/auth/login

POST   /api/documents/upload
GET    /api/documents
DELETE /api/documents/{id}

POST   /api/query
```

Example query:

```json
{
  "question": "What does the document say about annual leave?"
}
```

Conceptual response:

```json
{
  "answer": "Employees receive ...",
  "sources": []
}
```

---

## Learning Through Real Engineering Problems

One of the main goals of DocuMind is to understand the technology by actually implementing and debugging it.

Some of the problems encountered during development include:

### DataSource Configuration

The application initially failed during Spring Boot startup because the database configuration was incomplete.

This helped establish the runtime connection:

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

### Vector Persistence Type Mismatch

Embeddings could be generated successfully, but persistence failed when the Java/JDBC representation did not match PostgreSQL's `vector` column type.

This highlighted an important boundary:

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

Generating an embedding successfully does not automatically mean that the vector can be persisted correctly.

### Database Status Constraint

The application also encountered a mismatch between the document status represented in Java and the values accepted by a PostgreSQL CHECK constraint.

This demonstrated the importance of keeping application-level state and database-level constraints synchronized.

---

## Engineering Approach

For each major concept, I am following this learning process:

```text
Understand the Concept
        ↓
Understand Why It Exists
        ↓
Understand How It Works
        ↓
Implement It
        ↓
Test It
        ↓
Debug Problems
        ↓
Document What I Learned
```

The goal is to avoid treating AI systems as black boxes.

---

## Project Goals

DocuMind is being built to understand and implement:

- PDF document ingestion
- Text extraction
- Intelligent chunking
- Embedding generation
- Vector storage
- Semantic similarity search
- Top-K retrieval
- Context-aware LLM generation
- Citation-backed answers
- Secure REST APIs
- Error handling and resilience

---

## What I Am Learning

The most interesting part of this project has been discovering how many layers exist underneath a seemingly simple question-answering application.

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
```

Building this system is helping me move from simply **integrating AI APIs** toward understanding the underlying concepts that make AI applications work.

---

## Documentation

I am maintaining detailed engineering notes while building DocuMind, including:

- System design
- Architecture decisions
- Algorithms
- Code explanations
- RAG concepts
- Debugging
- Problems encountered
- Testing
- Interview preparation
- Lessons learned

---

## Author

**Gaurav Vishwakarma**

Building DocuMind as a practical learning journey from:

**Java / Spring Boot → AI Systems → LLM Integration → RAG → Embeddings → Vector Search**

> **Learning how to use the technology, while also understanding why it works.**
