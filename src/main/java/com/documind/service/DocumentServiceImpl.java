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
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    // Extracts raw text from the uploaded PDF.
    private final TikaTextExtractionService tikaTextExtractionService;

    // Splits the extracted text into smaller chunks.
    private final DocumentChunkingService documentChunkingService;

    // Repository used to store generated document chunks.
    private final DocumentChunkRepository documentChunkRepository;

    // Generates a Gemini embedding for each chunk.
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

        // ---------------------------------------------------------
        // STEP 1: Validate the uploaded file.
        // ---------------------------------------------------------
        validatePdf(file);


        // ---------------------------------------------------------
        // STEP 2: Verify that the user exists.
        // ---------------------------------------------------------
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );


        // ---------------------------------------------------------
        // STEP 3: Calculate SHA-256 hash of the PDF.
        // ---------------------------------------------------------
        String documentHash;

        try {

            documentHash = calculateSha256(file);

            log.info(
                    "Calculated SHA-256 hash for {}: {}",
                    file.getOriginalFilename(),
                    documentHash
            );

        } catch (IOException exception) {

            log.error(
                    "Failed to read PDF while calculating hash: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to process the uploaded PDF."
            );
        }


        // ---------------------------------------------------------
        // STEP 4: Check whether this user already uploaded
        // the exact same document.
        // ---------------------------------------------------------
        if (documentRepository
                .findByUserIdAndDocumentHash(
                        userId,
                        documentHash
                )
                .isPresent()) {

            log.warn(
                    "Duplicate document upload rejected for user {}: {}",
                    userId,
                    file.getOriginalFilename()
            );

            throw new InvalidDocumentException(
                    "This document has already been uploaded."
            );
        }


        // ---------------------------------------------------------
        // STEP 5: Extract raw text from the PDF using Apache Tika.
        // ---------------------------------------------------------
        String extractedText;

        try {

            extractedText =
                    tikaTextExtractionService.extractText(file);

            log.info(
                    "Successfully extracted {} characters from PDF: {} | blank={} | text=[{}]",
                    extractedText.length(),
                    file.getOriginalFilename(),
                    extractedText.isBlank(),
                    extractedText.replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t")
            );

        } catch (IOException exception) {

            log.error(
                    "Failed to read PDF: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to read the uploaded PDF."
            );

        } catch (TikaException exception) {

            log.error(
                    "Failed to parse PDF with Apache Tika: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to extract text from the uploaded PDF."
            );
        }


        // ---------------------------------------------------------
        // STEP 6: Divide the extracted text into chunks.
        // ---------------------------------------------------------
        List<String> chunks =
                documentChunkingService.chunk(extractedText);

        log.info(
                "Created {} text chunks from PDF: {}",
                chunks.size(),
                file.getOriginalFilename()
        );


        // ---------------------------------------------------------
        // STEP 7: Create the Document entity.
        // ---------------------------------------------------------
        Document document = new Document();

        document.setUser(user);
        document.setFilename(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());
        document.setStatus(DocumentStatus.PROCESSING);
        document.setDocumentHash(documentHash);


        // Save the parent document first.
        Document savedDocument =
                documentRepository.save(document);


        // ---------------------------------------------------------
        // STEP 8: Create DocumentChunk entities.
        // ---------------------------------------------------------
        List<DocumentChunk> documentChunks =
                new ArrayList<>();


        for (int i = 0; i < chunks.size(); i++) {

            String chunkText = chunks.get(i);


            // -----------------------------------------------------
            // STEP 9: Generate Gemini embedding for this chunk.
            // -----------------------------------------------------
            float[] embedding;

            try {

                embedding =
                        embeddingService.generateEmbedding(chunkText);

            } catch (RuntimeException exception) {

                log.error(
                        "Failed to generate embedding for chunk {} of document {} after retry attempts",
                        i,
                        savedDocument.getId(),
                        exception
                );

                throw new InvalidDocumentException(
                        "Unable to generate embedding for the uploaded document."
                );
            }


            // -----------------------------------------------------
            // STEP 10: Create DocumentChunk entity.
            // -----------------------------------------------------
            DocumentChunk documentChunk =
                    new DocumentChunk();

            documentChunk.setDocument(savedDocument);
            documentChunk.setChunkIndex(i);
            documentChunk.setContent(chunkText);
            documentChunk.setEmbedding(embedding);

            documentChunks.add(documentChunk);


            log.info(
                    "Generated embedding for chunk {} of document {}",
                    i,
                    savedDocument.getId()
            );
        }


        // ---------------------------------------------------------
        // STEP 11: Persist chunks + embeddings.
        // ---------------------------------------------------------
        documentChunkRepository.saveAll(documentChunks);

        log.info(
                "Persisted {} chunks with embeddings for document id: {}",
                documentChunks.size(),
                savedDocument.getId()
        );


        // ---------------------------------------------------------
        // STEP 12: Mark document as completed.
        // ---------------------------------------------------------
        savedDocument.setStatus(DocumentStatus.COMPLETED);

        documentRepository.save(savedDocument);

        log.info(
                "Document {} processing completed successfully",
                savedDocument.getId()
        );


        // ---------------------------------------------------------
        // STEP 13: Return upload response.
        // ---------------------------------------------------------
        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }


    // -------------------------------------------------------------
    // Calculates SHA-256 hash of the uploaded PDF.
    // -------------------------------------------------------------
    private String calculateSha256(
            MultipartFile file
    ) throws IOException {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(file.getBytes());

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            // SHA-256 is guaranteed to exist in standard Java.
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }


    // -------------------------------------------------------------
    // Validates that the uploaded file exists, is not empty,
    // and has a PDF content type.
    // -------------------------------------------------------------
    private void validatePdf(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new InvalidDocumentException(
                    "File must not be empty."
            );
        }

        String contentType =
                file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)) {

            throw new InvalidDocumentException(
                    "Only PDF files are allowed."
            );
        }
    }
}