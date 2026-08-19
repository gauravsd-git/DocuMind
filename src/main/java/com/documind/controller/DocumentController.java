package com.documind.controller;

import com.documind.dto.DocumentUploadResponse;
import com.documind.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(
        name = "Documents",
        description = "Document upload and ingestion APIs"
)
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
            summary = "Upload a PDF document",
            description = """
                    Uploads a PDF document for a user.

                    The uploaded PDF goes through the following ingestion pipeline:
                    1. PDF file validation
                    2. Text extraction using Apache Tika
                    3. Text chunking with overlapping chunks
                    4. Storage of document metadata
                    5. Storage of generated document chunks

                    Embeddings are not generated at this stage.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "PDF uploaded and processed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid, empty, or unsupported PDF file"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {

        // Delegate document processing to the service layer.
        DocumentUploadResponse response =
                documentService.upload(file, userId);

        // Return HTTP 201 Created with the document details.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}