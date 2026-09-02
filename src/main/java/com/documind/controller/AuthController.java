package com.documind.controller;

import com.documind.dto.AuthResponse;
import com.documind.dto.LoginRequest;
import com.documind.dto.RegisterRequest;
import com.documind.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "User registration and login APIs"
)
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new DocuMind user account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid registration request")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ) {

        AuthResponse response =
                authService.register(
                        request
                );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        response
                );
    }


    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns a JWT token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }
}