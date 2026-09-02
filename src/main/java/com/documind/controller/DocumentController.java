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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(
        name = "Documents",
        description = "Document upload and ingestion APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
            summary = "Upload a PDF document",
            description = """
                    Uploads a PDF document for the authenticated user.

                    The uploaded PDF goes through:
                    1. PDF file validation
                    2. Text extraction using Apache Tika
                    3. Text chunking with overlapping chunks
                    4. Document metadata storage
                    5. Document chunk storage
                    6. Gemini embedding generation
                    7. pgvector storage
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "PDF uploaded and processed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid, empty, duplicate, oversized, or unsupported PDF file"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
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
            Authentication authentication
    ) {

        // Get the user ID from the authenticated JWT.
        Long userId =
                (Long) authentication.getPrincipal();

        DocumentUploadResponse response =
                documentService.upload(
                        file,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
