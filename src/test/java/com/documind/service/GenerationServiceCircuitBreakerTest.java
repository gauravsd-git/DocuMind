package com.documind.service;

import com.documind.entity.DocumentChunk;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GenerationServiceCircuitBreakerTest {

    @Test
    void circuitBreakerShouldOpenAfterRepeatedGeminiFailures() {

        // Arrange
        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.call(anyString()))
                .thenThrow(new RuntimeException("Gemini unavailable"));

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry =
                CircuitBreakerRegistry.of(config);

        GenerationServiceImpl service =
                new GenerationServiceImpl(
                        chatModel,
                        registry
                );

        DocumentChunk chunk = mock(DocumentChunk.class);

        when(chunk.getContent())
                .thenReturn("This is test document content.");

        List<DocumentChunk> context = List.of(chunk);

        // Act
        for (int i = 0; i < 5; i++) {

            assertThrows(
                    RuntimeException.class,
                    () -> service.generateAnswer(
                            "What is this document about?",
                            context
                    )
            );
        }

        // Assert
        CircuitBreaker circuitBreaker =
                registry.circuitBreaker("gemini");

        assertEquals(
                State.OPEN,
                circuitBreaker.getState()
        );

        // Circuit is OPEN, so Gemini should NOT be called again.
        // Instead, the fallback response should be returned.
        String fallbackResponse = service.generateAnswer(
                "What is this document about?",
                context
        );

        assertEquals(
                "The AI service is temporarily unavailable. Please try again shortly.",
                fallbackResponse
        );

        // Verify Gemini was called only during the 5 failed attempts.
        verify(chatModel, times(5))
                .call(anyString());
    }
}