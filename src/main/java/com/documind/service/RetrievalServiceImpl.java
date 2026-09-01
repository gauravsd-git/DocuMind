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
            Long userId,
            String query,
            int topK
    ) {
        System.out.println("QUERY DEBUG | userId = " + userId
                + " | query = " + query);

        // ---------------------------------------------------------
        // STEP 1: Validate user.
        // ---------------------------------------------------------

        if (userId == null) {

            throw new IllegalArgumentException(
                    "User id must not be null."
            );
        }


        // ---------------------------------------------------------
        // STEP 2: Validate query.
        // ---------------------------------------------------------

        if (query == null || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Query must not be empty."
            );
        }


        // ---------------------------------------------------------
        // STEP 3: Validate topK.
        // ---------------------------------------------------------

        if (topK <= 0) {

            throw new IllegalArgumentException(
                    "topK must be greater than zero."
            );
        }


        // ---------------------------------------------------------
        // STEP 4: Generate embedding for user's question.
        // ---------------------------------------------------------

        float[] queryEmbedding =
                embeddingService.generateEmbedding(
                        query
                );


        // ---------------------------------------------------------
        // STEP 5: Convert float[] to PostgreSQL vector syntax.
        //
        // Example:
        //
        // [0.12,0.43,-0.21,...]
        // ---------------------------------------------------------

        String vector =
                toPgVector(queryEmbedding);


        // ---------------------------------------------------------
        // STEP 6: Search ONLY this user's documents.
        // ---------------------------------------------------------

        return documentChunkRepository.findSimilarChunks(
                userId,
                vector,
                topK
        );
    }


    private String toPgVector(
            float[] embedding
    ) {

        if (embedding == null ||
                embedding.length == 0) {

            throw new IllegalArgumentException(
                    "Embedding must not be empty."
            );
        }


        StringBuilder vector =
                new StringBuilder("[");


        for (int i = 0;
             i < embedding.length;
             i++) {

            if (i > 0) {
                vector.append(",");
            }

            vector.append(
                    embedding[i]
            );
        }


        vector.append("]");

        return vector.toString();
    }
}