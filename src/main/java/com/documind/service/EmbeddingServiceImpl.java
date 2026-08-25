package com.documind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    /*
     * Spring AI provides the EmbeddingModel abstraction.
     *
     * Spring AI connects this abstraction to Google's
     * Gemini embedding model.
     *
     * Spring AI also provides retry handling for
     * transient AI/API failures.
     */
    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot generate embedding for empty text"
            );
        }

        try {

            log.debug(
                    "Generating embedding for text of {} characters",
                    text.length()
            );

            float[] embedding = embeddingModel.embed(text);

            if (embedding.length == 0) {
                throw new IllegalStateException(
                        "Embedding API returned an empty vector"
                );
            }

            log.debug(
                    "Successfully generated embedding with {} dimensions",
                    embedding.length
            );

            return embedding;

        } catch (RuntimeException exception) {

            /*
             * Spring AI has already performed its configured
             * retry attempts for retryable transient failures.
             *
             * If execution reaches this block, the embedding
             * operation ultimately failed.
             */
            log.error(
                    "Embedding generation failed after API/retry processing",
                    exception
            );

            throw exception;
        }
    }
}