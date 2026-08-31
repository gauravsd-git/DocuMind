package com.documind.service;

import com.documind.dto.QueryResponse;
import com.documind.dto.QuerySource;
import com.documind.entity.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private static final int DEFAULT_TOP_K = 3;

    private final RetrievalService retrievalService;
    private final GenerationService generationService;

    @Override
    @Cacheable(
            value = "queryResponses",
            key = "#userId + ':' + #question.trim().toLowerCase()"
    )
    public QueryResponse query(
            Long userId,
            String question
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User id must not be null."
            );
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question must not be empty."
            );
        }

        // Retrieve the most relevant chunks belonging
        // only to the requested user.
        List<DocumentChunk> relevantChunks =
                retrievalService.retrieve(
                        userId,
                        question,
                        DEFAULT_TOP_K
                );

        // Generate an answer using only the retrieved chunks.
        String answer =
                generationService.generateAnswer(
                        question,
                        relevantChunks
                );

        /*
         * Convert retrieved chunks into citation information.
         */
        List<QuerySource> sources =
                relevantChunks.stream()
                        .map(chunk -> new QuerySource(
                                chunk.getDocument().getId(),
                                chunk.getDocument().getFilename(),
                                chunk.getChunkIndex()
                        ))
                        .distinct()
                        .toList();

        return new QueryResponse(
                answer,
                sources
        );
    }
}