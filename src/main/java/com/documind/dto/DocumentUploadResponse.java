package com.documind.dto;

import com.documind.entity.DocumentStatus;

public record DocumentUploadResponse(
        Long documentId,
        String filename,
        DocumentStatus status
) {
}