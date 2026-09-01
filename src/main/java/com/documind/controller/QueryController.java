package com.documind.controller;

import com.documind.dto.QueryRequest;
import com.documind.dto.QueryResponse;
import com.documind.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

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
