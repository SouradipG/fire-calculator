package com.fire.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FIRE Calculator — Spring Boot Application
 *
 * FIRE = Financial Independence, Retire Early
 *
 * This application exposes GraphQL APIs to calculate a user's FIRE number
 * and integrates with Groq (free LLM API) via Spring AI for intelligent,
 * personalized financial insights.
 *
 * Endpoints:
 *   - POST /graphql       → Main GraphQL API
 *   - GET  /graphiql      → Browser-based GraphQL IDE (dev)
 *
 * LLM: Groq (Llama-3.3-70b-versatile) via OpenAI-compatible API
 *   - Free tier: console.groq.com (no credit card required)
 *   - Spring AI integration via spring-ai-openai-spring-boot-starter
 */
@SpringBootApplication
public class FireCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireCalculatorApplication.class, args);
    }
}
