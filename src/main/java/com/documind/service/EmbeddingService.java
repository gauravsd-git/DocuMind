package com.documind.service;

public interface EmbeddingService {

    /**
     * Generates a vector embedding for the supplied text.
     *
     * @param text text that should be converted into an embedding
     * @return vector representation of the text
     */
    float[] generateEmbedding(String text);
}