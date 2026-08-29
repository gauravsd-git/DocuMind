package com.documind.controller;

import com.documind.dto.QueryRequest;
import com.documind.dto.QueryResponse;
import com.documind.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<QueryResponse> query(
            @RequestBody QueryRequest request
    ) {

        QueryResponse response =
                queryService.query(
                        request.userId(),
                        request.question()
                );

        return ResponseEntity.ok(response);
    }
}