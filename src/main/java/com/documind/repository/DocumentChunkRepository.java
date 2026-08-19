package com.documind.repository;

import com.documind.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {
}