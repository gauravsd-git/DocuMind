package com.documind.service;

import com.documind.dto.DocumentUploadResponse;
import com.documind.entity.Document;
import com.documind.entity.DocumentChunk;
import com.documind.entity.DocumentStatus;
import com.documind.entity.User;
import com.documind.exception.InvalidDocumentException;
import com.documind.exception.ResourceNotFoundException;
import com.documind.repository.DocumentChunkRepository;
import com.documind.repository.DocumentRepository;
import com.documind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl
        implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TikaTextExtractionService tikaTextExtractionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;


    @Override
    @Transactional
    @CacheEvict(
            value = "queryResponses",
            allEntries = true
    )
    public DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    ) {

        // =========================================================
        // STEP 1: Validate PDF.
        // =========================================================

        validatePdf(file);


        // =========================================================
        // STEP 2: Find authenticated user.
        // =========================================================

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id: "
                                                + userId
                                )
                        );


        // =========================================================
        // STEP 3: Calculate SHA-256.
        // =========================================================

        String documentHash;

        try {

            documentHash =
                    calculateSha256(file);

            log.info(
                    "SHA-256 for {} = {}",
                    file.getOriginalFilename(),
                    documentHash
            );

        } catch (IOException exception) {

            log.error(
                    "Failed to calculate SHA-256",
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to process the uploaded PDF."
            );
        }


        // =========================================================
        // STEP 4: Reject duplicate for same user.
        // =========================================================

        if (documentRepository
                .findByUserIdAndDocumentHash(
                        userId,
                        documentHash
                )
                .isPresent()) {

            throw new InvalidDocumentException(
                    "This document has already been uploaded."
            );
        }


        // =========================================================
        // STEP 5: Extract PDF text using Apache Tika.
        // =========================================================

        String extractedText;

        try {

            extractedText =
                    tikaTextExtractionService.extractText(
                            file
                    );

        } catch (IOException exception) {

            log.error(
                    "Failed to read PDF",
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to read the uploaded PDF."
            );

        } catch (TikaException exception) {

            log.error(
                    "Failed to extract PDF text",
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to extract text from the uploaded PDF."
            );
        }


        // =========================================================
        // STEP 6: Make chunks.
        // =========================================================

        List<String> chunks =
                documentChunkingService.chunk(
                        extractedText
                );


        if (chunks.isEmpty()) {

            throw new InvalidDocumentException(
                    "No readable text was found in the PDF."
            );
        }


        log.info(
                "Created {} chunks",
                chunks.size()
        );


        // =========================================================
        // STEP 7: Create Document entity.
        // =========================================================

        Document document =
                new Document();

        document.setUser(user);
        document.setFilename(
                file.getOriginalFilename()
        );
        document.setUploadDate(
                LocalDateTime.now()
        );
        document.setStatus(
                DocumentStatus.PROCESSING
        );
        document.setDocumentHash(
                documentHash
        );


        Document savedDocument =
                documentRepository.save(
                        document
                );


        // =========================================================
        // STEP 8: Create chunks + embeddings.
        // =========================================================

        List<DocumentChunk> documentChunks =
                new ArrayList<>();


        for (int i = 0;
             i < chunks.size();
             i++) {

            String chunkText =
                    chunks.get(i);


            float[] embedding;

            try {

                embedding =
                        embeddingService
                                .generateEmbedding(
                                        chunkText
                                );

            } catch (RuntimeException exception) {

                log.error(
                        "Embedding generation failed for chunk {}",
                        i,
                        exception
                );

                throw new InvalidDocumentException(
                        "Unable to generate embedding for the uploaded document."
                );
            }


            DocumentChunk documentChunk =
                    new DocumentChunk();

            documentChunk.setDocument(
                    savedDocument
            );

            documentChunk.setChunkIndex(
                    i
            );

            documentChunk.setContent(
                    chunkText
            );

            documentChunk.setEmbedding(
                    embedding
            );

            documentChunks.add(
                    documentChunk
            );
        }


        // =========================================================
        // STEP 9: Save chunks.
        // =========================================================

        documentChunkRepository.saveAll(
                documentChunks
        );


        // =========================================================
        // STEP 10: Mark document completed.
        // =========================================================

        savedDocument.setStatus(
                DocumentStatus.COMPLETED
        );

        documentRepository.save(
                savedDocument
        );


        log.info(
                "Document {} completed with {} chunks",
                savedDocument.getId(),
                documentChunks.size()
        );


        // =========================================================
        // STEP 11: Return response.
        // =========================================================

        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }


    // =============================================================
    // SHA-256
    // =============================================================

    private String calculateSha256(
            MultipartFile file
    ) throws IOException {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            file.getBytes()
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }


    // =============================================================
    // PDF validation
    // =============================================================

    private void validatePdf(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new InvalidDocumentException(
                    "File must not be empty."
            );
        }

        String filename =
                file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".pdf")) {

            throw new InvalidDocumentException(
                    "Only PDF files are allowed."
            );
        }

        try {

            byte[] fileBytes = file.getBytes();

            if (fileBytes.length < 5) {

                throw new InvalidDocumentException(
                        "Invalid PDF file."
                );
            }

            String pdfHeader =
                    new String(
                            fileBytes,
                            0,
                            5,
                            java.nio.charset.StandardCharsets.US_ASCII
                    );

            if (!"%PDF-".equals(pdfHeader)) {

                throw new InvalidDocumentException(
                        "Invalid PDF file."
                );
            }

        } catch (IOException exception) {

            throw new InvalidDocumentException(
                    "Unable to read the uploaded PDF."
            );
        }
    }
}