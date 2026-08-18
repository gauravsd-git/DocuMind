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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    ) {

        validatePdf(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        Document document = new Document();

        document.setUser(user);
        document.setFilename(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());
        document.setStatus(DocumentStatus.PROCESSING);

        Document savedDocument = documentRepository.save(document);

        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFilename(),
                savedDocument.getStatus()
        );
    }

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