package com.documind.service;

import com.documind.entity.DocumentChunk;

import java.util.List;

public interface GenerationService {

    String generateAnswer(
            String question,
            List<DocumentChunk> context
    );
}