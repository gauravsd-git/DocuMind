package com.documind.service;

import com.documind.dto.AuthResponse;
import com.documind.dto.LoginRequest;
import com.documind.dto.RegisterRequest;
import com.documind.entity.User;
import com.documind.repository.UserRepository;
import com.documind.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    @Override
    public AuthResponse register(
            RegisterRequest request
    ) {

        if (request.email() == null ||
                request.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Email must not be empty."
            );
        }


        if (request.password() == null ||
                request.password().isBlank()) {

            throw new IllegalArgumentException(
                    "Password must not be empty."
            );
        }


        if (userRepository.existsByEmail(
                request.email()
        )) {

            throw new IllegalArgumentException(
                    "Email is already registered."
            );
        }


        User user =
                new User();

        user.setEmail(
                request.email()
        );

        user.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setRole(
                "USER"
        );


        User savedUser =
                userRepository.save(
                        user
                );


        String token =
                jwtService.generateToken(
                        savedUser.getId(),
                        savedUser.getEmail(),
                        savedUser.getRole()
                );


        return new AuthResponse(
                token
        );
    }


    @Override
    public AuthResponse login(
            LoginRequest request
    ) {

        User user =
                userRepository
                        .findByEmail(
                                request.email()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid email or password."
                                )
                        );


        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Invalid email or password."
            );
        }


        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole()
                );


        return new AuthResponse(
                token
        );
    }
}