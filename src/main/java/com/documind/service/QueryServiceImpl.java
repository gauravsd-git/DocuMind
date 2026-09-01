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

        // ---------------------------------------------------------
        // STEP 1: Validate authenticated user.
        // ---------------------------------------------------------

        if (userId == null) {

            throw new IllegalArgumentException(
                    "Authenticated user id must not be null."
            );
        }


        // ---------------------------------------------------------
        // STEP 2: Validate question.
        // ---------------------------------------------------------

        if (question == null || question.isBlank()) {

            throw new IllegalArgumentException(
                    "Question must not be empty."
            );
        }


        // ---------------------------------------------------------
        // STEP 3: Retrieve relevant chunks.
        //
        // IMPORTANT:
        // userId comes from JWT authentication.
        //
        // Therefore retrieval can only search documents
        // belonging to the authenticated user.
        // ---------------------------------------------------------

        List<DocumentChunk> relevantChunks =
                retrievalService.retrieve(
                        userId,
                        question,
                        DEFAULT_TOP_K
                );


        // ---------------------------------------------------------
        // STEP 4: Generate answer from retrieved context.
        // ---------------------------------------------------------

        String answer =
                generationService.generateAnswer(
                        question,
                        relevantChunks
                );


        // ---------------------------------------------------------
        // STEP 5: Build citation/source information.
        // ---------------------------------------------------------

        List<QuerySource> sources =
                relevantChunks.stream()
                        .map(chunk ->
                                new QuerySource(
                                        chunk.getDocument().getId(),
                                        chunk.getDocument().getFilename(),
                                        chunk.getChunkIndex()
                                )
                        )
                        .distinct()
                        .toList();


        // ---------------------------------------------------------
        // STEP 6: Return answer + sources.
        // ---------------------------------------------------------

        return new QueryResponse(
                answer,
                sources
        );
    }
}