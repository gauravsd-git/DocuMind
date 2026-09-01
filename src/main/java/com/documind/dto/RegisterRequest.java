package com.documind.dto;

public record RegisterRequest(
        String email,
        String password
) {
}