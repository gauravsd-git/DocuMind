package com.documind.service;

import com.documind.entity.DocumentChunk;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.ai.chat.model.ChatModel;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerationServiceImpl implements GenerationService {

    private final CircuitBreaker geminiCircuitBreaker;
    private final ChatModel chatModel;

    public GenerationServiceImpl(
            ChatModel chatModel,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.chatModel = chatModel;
        this.geminiCircuitBreaker =
                circuitBreakerRegistry.circuitBreaker("gemini");
    }

    @Override
    public String generateAnswer(
            String question,
            List<DocumentChunk> context
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question must not be empty."
            );
        }

        if (context == null || context.isEmpty()) {
            return "I could not find relevant information in the uploaded documents.";
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are DocuMind, a document question-answering assistant.

                Answer the user's question using ONLY the provided context.

                If the answer cannot be found in the context,
                clearly state that the information is not available
                in the uploaded documents.

                Do not invent or assume information.

                CONTEXT:
                """);

        for (DocumentChunk chunk : context) {

            prompt.append("\n--- Document Chunk ---\n");
            prompt.append(chunk.getContent());
            prompt.append("\n");
        }

        prompt.append("\nQUESTION:\n");
        prompt.append(question);

        prompt.append("\n\nANSWER:");

        try {
            return geminiCircuitBreaker.executeSupplier(
                    () -> chatModel.call(prompt.toString())
            );
        } catch (CallNotPermittedException ex) {
            return fallbackResponse();
        }
    }

    private String fallbackResponse() {
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }
}