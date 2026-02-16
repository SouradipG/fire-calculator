package com.fire.calculator.graphql;

import com.fire.calculator.dto.*;
import com.fire.calculator.service.FireCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FireCalculatorResolver {

    private final FireCalculatorService fireCalculatorService;

    @QueryMapping
    public FireResultDTO calculateFire(@Argument @Valid FireInputDTO input) {
        log.info("GraphQL calculateFire invoked for: {}", input.getName());
        try {
            return fireCalculatorService.calculateFire(input);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error in calculateFire: {}", e.getMessage());
            throw new RuntimeException("Input validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in calculateFire for user: {}", input.getName(), e);
            throw new RuntimeException("FIRE calculation failed: " + e.getMessage());
        }
    }

    @QueryMapping
    public FireSummaryDTO quickFireCheck(@Argument @Valid FireInputDTO input) {
        log.info("GraphQL quickFireCheck invoked for: {}", input.getName());
        try {
            return fireCalculatorService.quickFireCheck(input);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Input validation failed: " + e.getMessage());
        }
    }
}
