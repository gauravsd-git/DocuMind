package com.documind.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmbeddingServiceImplTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void shouldGenerateEmbedding() {

        // Sample text that will be converted into a vector.
        String text = "PostgreSQL stores document chunks.";

        // Ask Gemini to generate the embedding.
        float[] embedding =
                embeddingService.generateEmbedding(text);

        // Verify that Gemini returned an embedding.
        assertNotNull(embedding);

        // The vector must contain at least one value.
        assertTrue(embedding.length > 0);

        // Print the dimension so we can see the actual
        // vector size returned by the configured model.
        System.out.println(
                "Embedding dimensions: " + embedding.length
        );

        // Print the first value only as a basic sanity check.
        System.out.println(
                "First embedding value: " + embedding[0]
        );
    }
}