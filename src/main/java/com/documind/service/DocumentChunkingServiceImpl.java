package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentChunkingServiceImpl
        implements DocumentChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    @Override
    public List<String> chunk(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] words = text.trim().split("\\s+");

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < words.length) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    words.length
            );

            String chunk = String.join(
                    " ",
                    Arrays.copyOfRange(words, start, end)
            );

            chunks.add(chunk);

            // Move forward while keeping an overlap
            // between the current chunk and the next chunk.
            if (end == words.length) {
                break;
            }

            start = end - OVERLAP;
        }

        return chunks;
    }
}