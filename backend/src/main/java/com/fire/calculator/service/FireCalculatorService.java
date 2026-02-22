package com.fire.calculator.service;

import com.fire.calculator.dto.*;
import com.fire.calculator.util.FireCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FireCalculatorService — Main Orchestration Layer
 *
 * Coordinates:
 *  1. Input validation and defaults resolution
 *  2. Deterministic FIRE math (via FireCalculationEngine)
 *  3. AI insight generation (via AiInsightsService → Groq)
 *  4. Result assembly into the GraphQL response types
 *
 * Single responsibility: orchestration. Math lives in FireCalculationEngine,
 * AI prompt logic in FirePromptBuilder, LLM call in AiInsightsService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FireCalculatorService {

    private final FireCalculationEngine calcEngine;
    private final AiInsightsService aiInsightsService;

    /**
     * Full FIRE analysis: deterministic calculations + AI insights.
     *
     * @param input User-provided FIRE inputs
     * @return Complete FireResultDTO with metrics and AI insights
     */
    public FireResultDTO calculateFire(FireInputDTO input) {
        log.info("Starting FIRE calculation for user: {}, age: {}, target: {}",
                input.getName(), input.getCurrentAge(), input.getTargetRetirementAge());

        validateInput(input);

        // Resolve effective rates (user-supplied or defaults)
        double returnRate   = calcEngine.effectiveReturnRate(input);
        double inflationRate = calcEngine.effectiveInflationRate(input);
        double withdrawalRate = calcEngine.effectiveWithdrawalRate(input);
        String currency = input.getCurrency() != null ? input.getCurrency() : "INR";

        // ── 1. Core Financial Calculations ──────────────────────────────────
        double fireNumber               = calcEngine.calculateFireNumber(input);
        double adjFireNumber            = calcEngine.calculateInflationAdjustedFireNumber(input);
        double projCorpus               = calcEngine.projectedCorpusAtRetirement(input);
        double shortfall                = calcEngine.corpusShortfall(input);
        double reqMonthlySip            = calcEngine.requiredMonthlySIP(input);
        double sipDelta                 = input.getMonthlySavings() - reqMonthlySip;
        double savingsRate              = calcEngine.savingsRate(input);
        double annualWithdrawal         = calcEngine.annualWithdrawalAtRetirement(input);
        boolean isOnTrack               = calcEngine.isOnTrack(input);
        int yearsToRetirement           = input.getTargetRetirementAge() - input.getCurrentAge();
        Integer projFireYear            = calcEngine.projectedFireYear(input);
        FireStatus status               = calcEngine.determineFireStatus(input);

        Integer fireYearDelta = null;
        if (projFireYear != null) {
            int targetYear = java.time.LocalDate.now().getYear() + yearsToRetirement;
            fireYearDelta = projFireYear - targetYear;
        }

        // ── 2. Year-by-Year Projection ──────────────────────────────────────
        List<ProjectionYearDTO> timeline = calcEngine.buildProjectionTimeline(input);

        // ── 3. AI Insights (Groq / Llama-3.3-70b) ──────────────────────────
        AiInsightsDTO aiInsights = aiInsightsService.generateInsights(
                input, fireNumber, adjFireNumber, projCorpus, shortfall,
                reqMonthlySip, sipDelta, savingsRate, annualWithdrawal,
                isOnTrack, yearsToRetirement, projFireYear, currency
        );

        log.info("FIRE calculation complete for: {}. OnTrack={}, FireNumber={}",
                input.getName(), isOnTrack,
                calcEngine.formatIndianCurrency(adjFireNumber, currency));

        // ── 4. Assemble Full Result ──────────────────────────────────────────
        return FireResultDTO.builder()
                .name(input.getName())
                .fireNumber(round(fireNumber))
                .fireNumberFormatted(calcEngine.formatIndianCurrency(adjFireNumber, currency))
                .corpusShortfall(round(shortfall))
                .projectedCorpusAtRetirement(round(projCorpus))
                .isOnTrack(isOnTrack)
                .yearsToRetirement(yearsToRetirement)
                .projectedFireYear(projFireYear)
                .fireYearDelta(fireYearDelta)
                .savingsRate(round(savingsRate))
                .requiredMonthlySip(round(reqMonthlySip))
                .monthlySipDelta(round(sipDelta))
                .inflationAdjustedFireNumber(round(adjFireNumber))
                .annualWithdrawalAtRetirement(round(annualWithdrawal))
                .projectionTimeline(timeline)
                .financialSnapshot(buildFinancialSnapshot(input, savingsRate,
                        returnRate, inflationRate, withdrawalRate))
                .aiInsights(aiInsights)
                .meta(buildMeta(withdrawalRate, inflationRate, returnRate, currency))
                .build();
    }

    /**
     * Quick FIRE check — no AI call. Pure calculation only.
     * Use for lightweight UI widgets, caching, or pre-flight checks.
     */
    public FireSummaryDTO quickFireCheck(FireInputDTO input) {
        log.info("Quick FIRE check for: {}", input.getName());
        validateInput(input);

        double fireNumber      = calcEngine.calculateInflationAdjustedFireNumber(input);
        boolean isOnTrack      = calcEngine.isOnTrack(input);
        int yearsToRetirement  = input.getTargetRetirementAge() - input.getCurrentAge();
        double reqMonthlySip   = calcEngine.requiredMonthlySIP(input);
        FireStatus status      = calcEngine.determineFireStatus(input);
        String currency        = input.getCurrency() != null ? input.getCurrency() : "INR";

        String summary = buildSummaryText(input, isOnTrack, status, reqMonthlySip, yearsToRetirement);

        return FireSummaryDTO.builder()
                .fireNumber(round(fireNumber))
                .isOnTrack(isOnTrack)
                .yearsToRetirement(yearsToRetirement)
                .requiredMonthlySip(round(reqMonthlySip))
                .status(status)
                .summary(summary)
                .build();
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void validateInput(FireInputDTO input) {
        if (!input.isRetirementAgeValid()) {
            throw new IllegalArgumentException(
                    "Target retirement age (" + input.getTargetRetirementAge() +
                    ") must be greater than current age (" + input.getCurrentAge() + ")");
        }
    }

    private FinancialSnapshotDTO buildFinancialSnapshot(
            FireInputDTO input, double savingsRate,
            double returnRate, double inflationRate, double withdrawalRate
    ) {
        double annualSavings = input.getMonthlySavings() * 12;
        return FinancialSnapshotDTO.builder()
                .annualIncome(input.getAnnualIncome())
                .annualExpenses(input.getAnnualExpenses())
                .annualSavings(annualSavings)
                .savingsRatePercent(round(savingsRate))
                .existingCorpus(input.getExistingCorpus())
                .monthlySavings(input.getMonthlySavings())
                .expectedReturnRatePercent(returnRate * 100)
                .inflationRatePercent(inflationRate * 100)
                .withdrawalRatePercent(withdrawalRate * 100)
                .build();
    }

    private CalculationMetaDTO buildMeta(
            double withdrawalRate, double inflationRate, double returnRate, String currency
    ) {
        return CalculationMetaDTO.builder()
                .calculatedAt(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .assumptions(AssumptionsDTO.builder()
                        .withdrawalRate(withdrawalRate)
                        .inflationRate(inflationRate)
                        .expectedReturnRate(returnRate)
                        .currency(currency)
                        .build())
                .build();
    }

    private String buildSummaryText(
            FireInputDTO input, boolean isOnTrack, FireStatus status,
            double reqMonthlySip, int yearsToRetirement
    ) {
        return switch (status) {
            case ALREADY_ACHIEVED -> String.format(
                    "%s has already achieved their FIRE number! You can retire now if desired.",
                    input.getName());
            case ON_TRACK -> String.format(
                    "%s is on track for FIRE in %d years. Keep your current SIP of ₹%.0f/month.",
                    input.getName(), yearsToRetirement, input.getMonthlySavings());
            case SLIGHTLY_BEHIND -> String.format(
                    "%s is slightly behind. Increase monthly SIP to ₹%.0f to get back on track.",
                    input.getName(), reqMonthlySip);
            case SIGNIFICANTLY_BEHIND -> String.format(
                    "%s is significantly behind. A monthly SIP of ₹%.0f is needed. " +
                    "Consider increasing income or extending retirement age.",
                    input.getName(), reqMonthlySip);
            case CRITICAL -> String.format(
                    "%s needs urgent action. Required SIP of ₹%.0f is very high. " +
                    "Consider extending retirement age, reducing expenses, or boosting income.",
                    input.getName(), reqMonthlySip);
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
