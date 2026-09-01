package com.documind.repository;

import com.documind.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    @Query(
            value = """
                    SELECT dc.*
                    FROM document_chunks dc
                    JOIN documents d
                        ON d.id = dc.document_id
                    WHERE d.user_id = :userId
                      AND dc.embedding IS NOT NULL
                    ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                    LIMIT :topK
                    """,
            nativeQuery = true
    )
    List<DocumentChunk> findSimilarChunks(
            @Param("userId") Long userId,
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );
}