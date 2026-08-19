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
    private final TikaTextExtractionService tikaTextExtractionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentChunkRepository documentChunkRepository;


    @Override
    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    ) {

        // Validate the uploaded file
        validatePdf(file);


        // Verify that the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );


        // Extract raw text from the PDF using Apache Tika
        String extractedText;

        try {

            extractedText =
                    tikaTextExtractionService.extractText(file);

            log.info(
                    "Successfully extracted {} characters from PDF: {}",
                    extractedText.length(),
                    file.getOriginalFilename()
            );

        } catch (IOException exception) {

            // IOException means that the PDF could not be read.
            log.error(
                    "Failed to read PDF: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to read the uploaded PDF."
            );

        } catch (TikaException exception) {

            /*
             * TikaException means that Apache Tika could not
             * successfully parse the PDF.
             */
            log.error(
                    "Failed to parse PDF with Apache Tika: {}",
                    file.getOriginalFilename(),
                    exception
            );

            throw new InvalidDocumentException(
                    "Unable to extract text from the uploaded PDF."
            );
        }


        // Divide the extracted text into chunks.
        List<String> chunks =
                documentChunkingService.chunk(extractedText);

        log.info(
                "Created {} text chunks from PDF: {}",
                chunks.size(),
                file.getOriginalFilename()
        );


        // Create the Document entity.
        Document document = new Document();

        document.setUser(user);
        document.setFilename(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());

        // The document is currently being processed.
        document.setStatus(DocumentStatus.PROCESSING);


        // Persist the Document.
        Document savedDocument = documentRepository.save(document);

        // Convert text chunks into DocumentChunk entities.
        List<DocumentChunk> documentChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {

            DocumentChunk documentChunk = new DocumentChunk();

            // Associate the chunk with its parent document.
            documentChunk.setDocument(savedDocument);

            // Store the position of the chunk.
            documentChunk.setChunkIndex(i);

            // Store the actual extracted text.
            documentChunk.setContent(chunks.get(i));
            documentChunks.add(documentChunk);
        }


        // Persist all chunks.
        documentChunkRepository.saveAll(documentChunks);

        log.info(
                "Persisted {} chunks for document id: {}",
                documentChunks.size(),
                savedDocument.getId()
        );


        // Return the upload response.
        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }


    // PDF VALIDATION.
    private void validatePdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new InvalidDocumentException(
                    "File must not be empty."
            );
        }

        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)) {

            throw new InvalidDocumentException(
                    "Only PDF files are allowed."
            );
        }
    }
}