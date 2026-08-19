package com.documind.service;

import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentChunkingServiceImplTest {

    private final DocumentChunkingServiceImpl chunkingService =
            new DocumentChunkingServiceImpl();


    @Test
    public void shouldReturnEmptyListWhenTextIsNull() {

        List<String> chunks = chunkingService.chunk(null);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }


    @Test
    public void shouldReturnEmptyListWhenTextIsBlank() {

        List<String> chunks = chunkingService.chunk("   ");

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }


    @Test
    public void shouldCreateSingleChunkForTextWithLessThan500Words() {

        String text = createWords(100);

        List<String> chunks = chunkingService.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals(100, countWords(chunks.getFirst()));
    }


    @Test
    public void shouldCreateExactlyOneChunkForTextWith500Words() {

        String text = createWords(500);

        List<String> chunks = chunkingService.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals(500, countWords(chunks.getFirst()));
    }


    @Test
    public void shouldCreateMultipleChunksForTextWithMoreThan500Words() {

        String text = createWords(1000);

        List<String> chunks = chunkingService.chunk(text);

        assertTrue(chunks.size() > 1);
    }


    @Test
    public void shouldCreateChunksWithMaximum500Words() {

        String text = createWords(1200);

        List<String> chunks = chunkingService.chunk(text);

        for (String chunk : chunks) {

            assertTrue(
                    countWords(chunk) <= 500,
                    "A chunk contains more than 500 words."
                        );
        }
    }


    @Test
    public void shouldMaintain50WordOverlapBetweenChunks() {

        String text = createWords(1000);

        List<String> chunks = chunkingService.chunk(text);

        assertTrue(chunks.size() > 1);

        String firstChunk = chunks.get(0);
        String secondChunk = chunks.get(1);

        String[] firstWords = firstChunk.split("\\s+");
        String[] secondWords = secondChunk.split("\\s+");

        /*
         * The first chunk contains words 1-500.
         * The second chunk should begin at word 451.
         *
         * Therefore, the last 50 words of the first chunk
         * should be the first 50 words of the second chunk.
         */
        for (int i = 0; i < 50; i++) {

            String wordFromFirstChunk =
                    firstWords[firstWords.length - 50 + i];

            String wordFromSecondChunk =
                    secondWords[i];

            assertEquals(
                    wordFromFirstChunk,
                    wordFromSecondChunk
            );
        }
    }


    @Test
    public void shouldPreserveFirstWordOfDocument() {

        String text = createWords(1000);

        List<String> chunks = chunkingService.chunk(text);

        assertTrue(
                chunks.getFirst().startsWith("word1")
        );
    }


    @Test
    public void shouldPreserveLastWordOfDocument() {

        String text = createWords(1000);

        List<String> chunks = chunkingService.chunk(text);

        String lastChunk =
                chunks.getLast();

        assertTrue(
                lastChunk.endsWith("word1000")
        );
    }


    @Test
    public void shouldPreserveWordOrder() {

        String text = createWords(1000);

        List<String> chunks = chunkingService.chunk(text);

        // Verify that the first chunk begins with word1.
        assertTrue(
                chunks.getFirst().startsWith("word1")
        );

        // Verify that the final chunk ends with word1000.
        assertTrue(
                chunks.getLast()
                        .endsWith("word1000")
        );
    }


    /**
     * Creates predictable test text.
     * Example: word1 word2 word3 ... word1000
     */
    private String createWords(int numberOfWords) {

        StringBuilder text = new StringBuilder();

        for (int i = 1; i <= numberOfWords; i++) {

            if (i > 1) {
                text.append(" ");
            }

            text.append("word").append(i);
        }

        return text.toString();
    }


    // Counts the number of words in a chunk.
    private int countWords(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }
}