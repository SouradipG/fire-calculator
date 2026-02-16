package com.fire.calculator.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Primary input DTO for FIRE number calculation.
 *
 * Maps directly to GraphQL's FireInputDTO input type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireInputDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Current age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 80, message = "Age must be at most 80")
    private Integer currentAge;

    @NotNull(message = "Target retirement age is required")
    @Min(value = 25, message = "Retirement age must be at least 25")
    @Max(value = 90, message = "Retirement age must be at most 90")
    private Integer targetRetirementAge;

    @NotNull(message = "Annual income is required")
    @Positive(message = "Annual income must be positive")
    private Double annualIncome;

    @NotNull(message = "Annual expenses are required")
    @Positive(message = "Annual expenses must be positive")
    private Double annualExpenses;

    @NotNull(message = "Existing corpus is required")
    @PositiveOrZero(message = "Existing corpus cannot be negative")
    private Double existingCorpus;

    @NotNull(message = "Monthly savings is required")
    @PositiveOrZero(message = "Monthly savings cannot be negative")
    private Double monthlySavings;

    /**
     * Expected annual portfolio return rate (e.g. 0.12 = 12%).
     * Defaults to 12% if null (reasonable for Indian equity mutual funds long-term).
     */
    @DecimalMin(value = "0.01", message = "Expected return rate must be at least 1%")
    @DecimalMax(value = "0.50", message = "Expected return rate cannot exceed 50%")
    private Double expectedReturnRate;

    /**
     * Expected annual inflation rate (e.g. 0.06 = 6%).
     * Defaults to 6% if null (India's average CPI-based inflation).
     */
    @DecimalMin(value = "0.01", message = "Inflation rate must be at least 1%")
    @DecimalMax(value = "0.30", message = "Inflation rate cannot exceed 30%")
    private Double inflationRate;

    /**
     * Safe withdrawal rate at retirement (e.g. 0.04 = 4%).
     * Based on Trinity Study research. Defaults to 4%.
     */
    @DecimalMin(value = "0.02", message = "Withdrawal rate must be at least 2%")
    @DecimalMax(value = "0.10", message = "Withdrawal rate cannot exceed 10%")
    private Double withdrawalRate;

    @Builder.Default
    private String currency = "INR";

    /** Optional description of retirement goal for AI context */
    private String retirementGoal;

    /** Optional extra expense categories beyond base living expenses */
    private List<AdditionalExpenseDTO> additionalExpenses;

    /** Investor risk tolerance for AI investment strategy advice */
    private RiskProfile riskProfile;

    /** Whether to request full detailed AI analysis vs quick summary */
    @Builder.Default
    private Boolean detailedAnalysis = true;

    // ── Validation ──────────────────────────────────────────────────────────

    public boolean isRetirementAgeValid() {
        return targetRetirementAge != null && currentAge != null
                && targetRetirementAge > currentAge;
    }

    public boolean hasSufficientSavings() {
        return annualIncome != null && annualExpenses != null
                && annualIncome > annualExpenses;
    }
}
