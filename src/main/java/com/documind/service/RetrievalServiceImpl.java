package com.documind.service;

import com.documind.entity.DocumentChunk;
import com.documind.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    @Override
    public List<DocumentChunk> retrieve(
            String query,
            int topK
    ) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Query must not be empty."
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "topK must be greater than zero."
            );
        }

        // 1. Convert the user's question into a Gemini embedding.
        float[] queryEmbedding =
                embeddingService.generateEmbedding(query);

        // 2. Convert Gemini's float[] into PostgreSQL vector format.
        String vector =
                toPgVector(queryEmbedding);

        // 3. Ask pgvector for the most similar chunks.
        return documentChunkRepository.findSimilarChunks(
                vector,
                topK
        );
    }

    private String toPgVector(float[] embedding) {

        StringBuilder vector =
                new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {

            if (i > 0) {
                vector.append(",");
            }

            vector.append(embedding[i]);
        }

        vector.append("]");

        return vector.toString();
    }
}