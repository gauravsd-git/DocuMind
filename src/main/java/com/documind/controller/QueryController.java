package com.documind.controller;

import com.documind.dto.QueryRequest;
import com.documind.dto.QueryResponse;
import com.documind.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Query",
        description = "Ask questions about the authenticated user's uploaded documents"
)
public class QueryController {

    private final QueryService queryService;

    @Operation(
            summary = "Ask a question",
            description = """
                Answers a question using only the authenticated user's
                uploaded documents.

                The system generates a query embedding, performs vector
                similarity search using pgvector, retrieves relevant chunks,
                and generates a grounded answer using Gemini.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question answered successfully"),
            @ApiResponse(responseCode = "400", description = "Question is empty or invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ResponseEntity<QueryResponse> query(
            @RequestBody QueryRequest request,
            Authentication authentication
    ) {

        // The JWT authentication filter stores the
        // authenticated user's ID as the principal.
        Long userId =
                (Long) authentication.getPrincipal();

        QueryResponse response =
                queryService.query(
                        userId,
                        request.question()
                );

        return ResponseEntity.ok(response);
    }
}
