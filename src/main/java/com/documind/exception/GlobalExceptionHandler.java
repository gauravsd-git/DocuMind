package com.documind.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<com.documind.exception.ErrorResponse> handleNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new com.documind.exception.ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        exception.getMessage(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocument(
            InvalidDocumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new com.documind.exception.ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        exception.getMessage(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        exception.getMessage(),
                        LocalDateTime.now()
                ));
    }
}