package com.documind.service;

import com.documind.dto.QueryResponse;
import com.documind.entity.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private static final int DEFAULT_TOP_K = 5;

    private final RetrievalService retrievalService;
    private final GenerationService generationService;

    @Override
    public QueryResponse query(
            Long userId, String question
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

        // Retrieve relevant chunks belonging ONLY to this user.
        List<DocumentChunk> relevantChunks =
                retrievalService.retrieve(
                        userId,
                        question,
                        DEFAULT_TOP_K
                );

        // Generate an answer using only the retrieved context.
        String answer =
                generationService.generateAnswer(
                        question,
                        relevantChunks
                );

        return new QueryResponse(answer);
    }
}