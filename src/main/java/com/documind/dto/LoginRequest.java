package com.documind.dto;

public record LoginRequest(
        String email,
        String password
) {
}