package com.documind.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    /*
     * Spring AI provides the EmbeddingModel abstraction.
     * Spring AI connects this abstraction
     * to Google's Gemini embedding model.
     */
    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateEmbedding(String text) {

        // Ask the configured embedding model to convert
        // the supplied text into a numerical vector.
        return embeddingModel.embed(text);
    }
}