package com.documind.service;

import com.documind.entity.DocumentChunk;

import java.util.List;

public interface RetrievalService {

    List<DocumentChunk> retrieve(
            Long userId,
            String query,
            int topK
    );
}