package com.documind.service;

import com.documind.dto.DocumentUploadResponse;
import com.documind.entity.Document;
import com.documind.entity.DocumentStatus;
import com.documind.entity.User;
import com.documind.exception.InvalidDocumentException;
import com.documind.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TikaTextExtractionService tikaTextExtractionService;

    @Override
    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    ) {

        // Validate that the uploaded file is a PDF.
        validatePdf(file);

        // Verify that the user exists.
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        // Extract the raw text from the PDF.
        String extractedText;

        try {

            extractedText = tikaTextExtractionService.extractText(file);

            // For now, we only verify that extraction succeeded.
            log.info(
                    "Successfully extracted {} characters from PDF: {}",
                    extractedText.length(),
                    file.getOriginalFilename()
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

        // Create the Document metadata entity.
        Document document = new Document();

        document.setUser(user);
        document.setFilename(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());
        document.setStatus(DocumentStatus.PROCESSING);

        // Persist document metadata.
        Document savedDocument = documentRepository.save(document);

        // Return the upload response.
        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }

    // Validates that the uploaded file exists, is not empty, and is a PDF.
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