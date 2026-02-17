package com.fire.calculator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Application Configuration
 *
 * Wires:
 *  - ObjectMapper (for JSON parsing of AI responses)
 *  - GraphQL runtime configuration
 */
@Configuration
public class AppConfig {

    /**
     * Jackson ObjectMapper bean for parsing Groq's JSON responses.
     * Configured for lenient deserialization to handle LLM response variations.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * GraphQL runtime wiring — currently uses schema-based auto-wiring.
     * Needs to be extended€ here for custom scalar types (e.g. Date, BigDecimal) if needed.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> {
        };
    }
}
