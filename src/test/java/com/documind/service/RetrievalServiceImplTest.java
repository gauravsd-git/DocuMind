package com.documind.service;

import com.documind.entity.DocumentChunk;
import com.documind.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetrievalServiceImplTest {

    private EmbeddingService embeddingService;
    private DocumentChunkRepository documentChunkRepository;
    private RetrievalServiceImpl retrievalService;

    @BeforeEach
    void setUp() {

        embeddingService = mock(EmbeddingService.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);

        retrievalService =
                new RetrievalServiceImpl(
                        embeddingService,
                        documentChunkRepository
                );
    }

    @Test
    void shouldRetrieveTopKRelevantChunks() {

        Long userId = 1L;
        String query = "What skills does the user have?";
        int topK = 5;

        float[] queryEmbedding = {
                0.1f,
                0.2f,
                0.3f
        };

        DocumentChunk chunk1 = new DocumentChunk();
        DocumentChunk chunk2 = new DocumentChunk();

        List<DocumentChunk> expectedChunks =
                List.of(chunk1, chunk2);

        when(embeddingService.generateEmbedding(query))
                .thenReturn(queryEmbedding);

        when(documentChunkRepository.findSimilarChunks(
                eq(userId),
                anyString(),
                eq(topK)
        )).thenReturn(expectedChunks);

        List<DocumentChunk> result =
                retrievalService.retrieve(
                        userId,
                        query,
                        topK
                );

        assertEquals(2, result.size());
        assertSame(chunk1, result.get(0));
        assertSame(chunk2, result.get(1));

        verify(embeddingService)
                .generateEmbedding(query);

        verify(documentChunkRepository)
                .findSimilarChunks(
                        eq(userId),
                        eq("[0.1,0.2,0.3]"),
                        eq(topK)
                );
    }

    @Test
    void shouldPassCorrectUserIdToRepository() {

        Long userId = 42L;
        String query = "What is the project about?";
        int topK = 3;

        float[] queryEmbedding = {
                0.5f,
                0.6f
        };

        List<DocumentChunk> chunks =
                List.of(new DocumentChunk());

        when(embeddingService.generateEmbedding(query))
                .thenReturn(queryEmbedding);

        when(documentChunkRepository.findSimilarChunks(
                eq(userId),
                anyString(),
                eq(topK)
        )).thenReturn(chunks);

        retrievalService.retrieve(
                userId,
                query,
                topK
        );

        verify(documentChunkRepository)
                .findSimilarChunks(
                        eq(42L),
                        eq("[0.5,0.6]"),
                        eq(3)
                );
    }

    @Test
    void shouldReturnEmptyListWhenNoChunksAreFound() {

        Long userId = 1L;
        String query = "Something not present";
        int topK = 5;

        float[] queryEmbedding = {
                0.1f,
                0.2f
        };

        when(embeddingService.generateEmbedding(query))
                .thenReturn(queryEmbedding);

        when(documentChunkRepository.findSimilarChunks(
                eq(userId),
                anyString(),
                eq(topK)
        )).thenReturn(List.of());

        List<DocumentChunk> result =
                retrievalService.retrieve(
                        userId,
                        query,
                        topK
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNullUserId() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> retrievalService.retrieve(
                                null,
                                "What is this document about?",
                                5
                        )
                );

        assertEquals(
                "User id must not be null.",
                exception.getMessage()
        );

        verifyNoInteractions(embeddingService);
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void shouldRejectBlankQuery() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> retrievalService.retrieve(
                                1L,
                                "   ",
                                5
                        )
                );

        assertEquals(
                "Query must not be empty.",
                exception.getMessage()
        );

        verifyNoInteractions(embeddingService);
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void shouldRejectNullQuery() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> retrievalService.retrieve(
                                1L,
                                null,
                                5
                        )
                );

        assertEquals(
                "Query must not be empty.",
                exception.getMessage()
        );

        verifyNoInteractions(embeddingService);
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void shouldRejectInvalidTopK() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> retrievalService.retrieve(
                                1L,
                                "What is this document about?",
                                0
                        )
                );

        assertEquals(
                "topK must be greater than zero.",
                exception.getMessage()
        );

        verifyNoInteractions(embeddingService);
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void shouldConvertEmbeddingToPgVectorFormat() {

        Long userId = 1L;
        String query = "test";
        int topK = 2;

        float[] embedding = {
                1.0f,
                -0.5f,
                0.25f
        };

        when(embeddingService.generateEmbedding(query))
                .thenReturn(embedding);

        when(documentChunkRepository.findSimilarChunks(
                anyLong(),
                anyString(),
                anyInt()
        )).thenReturn(List.of());

        retrievalService.retrieve(
                userId,
                query,
                topK
        );

        verify(documentChunkRepository)
                .findSimilarChunks(
                        eq(userId),
                        eq("[1.0,-0.5,0.25]"),
                        eq(topK)
                );
    }
}