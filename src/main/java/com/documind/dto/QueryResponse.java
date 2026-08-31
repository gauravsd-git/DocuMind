package com.documind.dto;

import java.util.List;

public record QueryResponse(
        String answer,
        List<QuerySource> sources
) {
}