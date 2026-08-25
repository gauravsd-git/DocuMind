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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // Repository used to store the generated document chunks.
    private final DocumentChunkRepository documentChunkRepository;

    // Generates a Gemini embedding for each chunk.
    private final EmbeddingService embeddingService;


    @Override
    @Transactional
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
        // STEP 3: Extract raw text from the PDF using Apache Tika.
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

            // The PDF could not be read.
            log.error(
                    "Failed to read PDF: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to read the uploaded PDF."
            );

        } catch (TikaException exception) {

            // Apache Tika could not parse the PDF.
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
        // STEP 4: Divide the extracted text into chunks.
        // ---------------------------------------------------------
        List<String> chunks =
                documentChunkingService.chunk(extractedText);

        log.info(
                "Created {} text chunks from PDF: {}",
                chunks.size(),
                file.getOriginalFilename()
        );


        // ---------------------------------------------------------
        // STEP 5: Create the Document entity.
        // ---------------------------------------------------------
        Document document = new Document();

        document.setUser(user);
        document.setFilename(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());

        // The document is still being processed.
        document.setStatus(DocumentStatus.PROCESSING);


        // Save the parent document first.
        Document savedDocument =
                documentRepository.save(document);


        // ---------------------------------------------------------
        // STEP 6: Create DocumentChunk entities.
        // ---------------------------------------------------------
        List<DocumentChunk> documentChunks =
                new ArrayList<>();


        for (int i = 0; i < chunks.size(); i++) {

            // Get the current chunk's text.
            String chunkText = chunks.get(i);


            // -----------------------------------------------------
            // STEP 7: Generate Gemini embedding for this chunk.
            // -----------------------------------------------------
            //
            // Gemini converts the chunk's text into a numerical
            // vector. In our current configuration this is:
            //
            //       float[3072]
            //
            // This vector represents the semantic meaning of
            // the chunk and will later allow similarity search.
            //
            float[] embedding =
                    embeddingService.generateEmbedding(chunkText);


            // -----------------------------------------------------
            // STEP 8: Create the DocumentChunk entity.
            // -----------------------------------------------------
            DocumentChunk documentChunk =
                    new DocumentChunk();

            // Associate the chunk with its parent document.
            documentChunk.setDocument(savedDocument);

            // Store the chunk's position in the document.
            documentChunk.setChunkIndex(i);

            // Store the original chunk text.
            documentChunk.setContent(chunkText);

            // Store Gemini's vector embedding.
            documentChunk.setEmbedding(embedding);


            // Add the completed chunk to the list.
            documentChunks.add(documentChunk);


            log.info(
                    "Generated embedding for chunk {} of document {}",
                    i,
                    savedDocument.getId()
            );
        }


        // ---------------------------------------------------------
        // STEP 9: Persist chunks + embeddings in PostgreSQL.
        // ---------------------------------------------------------
        //
        // Each DocumentChunk now contains:
        //
        //   document
        //   chunkIndex
        //   content
        //   embedding
        //
        // PostgreSQL stores the embedding as:
        //
        //   vector(3072)
        //
        documentChunkRepository.saveAll(documentChunks);

        log.info(
                "Persisted {} chunks with embeddings for document id: {}",
                documentChunks.size(),
                savedDocument.getId()
        );

        // Processing completed successfully.
        savedDocument.setStatus(DocumentStatus.COMPLETED);
        documentRepository.save(savedDocument);

        log.info(
                "Document {} processing completed successfully",
                savedDocument.getId()
        );


        // ---------------------------------------------------------
        // STEP 10: Return the upload response.
        // ---------------------------------------------------------
        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }


    // -------------------------------------------------------------
    // Validates that the uploaded file exists, is not empty,
    // and has a PDF content type.
    // -------------------------------------------------------------
    private void validatePdf(MultipartFile file) {

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