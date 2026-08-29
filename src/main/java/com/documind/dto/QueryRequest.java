package com.documind.dto;

public record QueryRequest(
        Long userId,
        String question
) {
}