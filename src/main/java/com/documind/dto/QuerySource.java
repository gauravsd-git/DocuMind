package com.documind.dto;

public record QuerySource(
        Long documentId,
        String filename,
        Integer chunkIndex
) {
}