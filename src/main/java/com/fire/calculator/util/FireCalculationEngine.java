package com.fire.calculator.util;

import com.fire.calculator.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Core FIRE financial calculation engine.
 *
 * Handles all deterministic math:
 *  - FIRE number computation (annual expenses / withdrawal rate)
 *  - Inflation-adjusted target corpus
 *  - Future value of current corpus (compound growth)
 *  - Required monthly SIP (PMT formula)
 *  - Year-by-year projection timeline
 *  - Projected FIRE year
 *
 * NO AI involvement here — this layer is pure math, ensuring correctness
 * and testability independently of the LLM layer.
 */
@Component
public class FireCalculationEngine {

    @Value("${fire.calculator.default-withdrawal-rate:0.04}")
    private double defaultWithdrawalRate;

    @Value("${fire.calculator.default-inflation-rate:0.06}")
    private double defaultInflationRate;

    @Value("${fire.calculator.default-return-rate:0.12}")
    private double defaultReturnRate;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Resolve effective rates — use user-supplied or fall back to defaults.
     */
    public double effectiveWithdrawalRate(FireInputDTO input) {
        return input.getWithdrawalRate() != null ? input.getWithdrawalRate() : defaultWithdrawalRate;
    }

    public double effectiveInflationRate(FireInputDTO input) {
        return input.getInflationRate() != null ? input.getInflationRate() : defaultInflationRate;
    }

    public double effectiveReturnRate(FireInputDTO input) {
        return input.getExpectedReturnRate() != null ? input.getExpectedReturnRate() : defaultReturnRate;
    }

    /**
     * Total annual expenses including additional goal expenses.
     */
    public double totalAnnualExpenses(FireInputDTO input) {
        double base = input.getAnnualExpenses();
        if (input.getAdditionalExpenses() != null) {
            base += input.getAdditionalExpenses().stream()
                    .mapToDouble(AdditionalExpenseDTO::getAnnualAmount)
                    .sum();
        }
        return base;
    }

    /**
     * FIRE Number = Annual Expenses / Safe Withdrawal Rate
     *
     * Example: ₹12L expenses / 4% SWR = ₹3 Crore FIRE number
     * This is the corpus needed TODAY (in today's money).
     */
    public double calculateFireNumber(FireInputDTO input) {
        return totalAnnualExpenses(input) / effectiveWithdrawalRate(input);
    }

    /**
     * Inflation-adjusted FIRE number — how much that corpus needs to be
     * at the future retirement date in future rupees.
     *
     * Formula: FIRE_today × (1 + inflation)^years
     */
    public double calculateInflationAdjustedFireNumber(FireInputDTO input) {
        double fireToday = calculateFireNumber(input);
        int years = input.getTargetRetirementAge() - input.getCurrentAge();
        double inflation = effectiveInflationRate(input);
        return fireToday * Math.pow(1 + inflation, years);
    }

    /**
     * Future Value of existing corpus at retirement.
     * FV = PV × (1 + r)^n
     */
    public double futureValueOfExistingCorpus(FireInputDTO input) {
        int years = input.getTargetRetirementAge() - input.getCurrentAge();
        double r = effectiveReturnRate(input);
        return input.getExistingCorpus() * Math.pow(1 + r, years);
    }

    /**
     * Future Value of monthly SIP contributions.
     *
     * Formula (FV of annuity due):
     *   FV = PMT × [((1+r/12)^n - 1) / (r/12)] × (1 + r/12)
     *
     * where n = months, r = annual rate, PMT = monthly SIP
     */
    public double futureValueOfMonthlySIP(FireInputDTO input) {
        int years = input.getTargetRetirementAge() - input.getCurrentAge();
        int months = years * 12;
        double monthlyRate = effectiveReturnRate(input) / 12;
        double pmt = input.getMonthlySavings();

        if (monthlyRate == 0) {
            return pmt * months;
        }

        return pmt * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate)
                * (1 + monthlyRate);
    }

    /**
     * Projected corpus at retirement = FV(existing corpus) + FV(monthly SIPs)
     */
    public double projectedCorpusAtRetirement(FireInputDTO input) {
        return futureValueOfExistingCorpus(input) + futureValueOfMonthlySIP(input);
    }

    /**
     * Corpus shortfall = inflationAdjustedFireNumber - projectedCorpus
     * Positive = shortfall, Negative = surplus
     */
    public double corpusShortfall(FireInputDTO input) {
        return calculateInflationAdjustedFireNumber(input) - projectedCorpusAtRetirement(input);
    }

    /**
     * Required monthly SIP to reach the inflation-adjusted FIRE number
     * by target retirement age.
     *
     * PMT formula (solve for PMT given FV):
     *   PMT = (FV - FV_existing) / [((1+r/12)^n - 1) / (r/12)] / (1 + r/12)
     */
    public double requiredMonthlySIP(FireInputDTO input) {
        double targetCorpus = calculateInflationAdjustedFireNumber(input);
        double fvExisting = futureValueOfExistingCorpus(input);
        double remainingNeeded = Math.max(0, targetCorpus - fvExisting);

        int years = input.getTargetRetirementAge() - input.getCurrentAge();
        int months = years * 12;
        double monthlyRate = effectiveReturnRate(input) / 12;

        if (monthlyRate == 0 || months == 0) {
            return months == 0 ? remainingNeeded : remainingNeeded / months;
        }

        double annuityFactor = ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate)
                * (1 + monthlyRate);

        return remainingNeeded / annuityFactor;
    }

    /**
     * Savings rate = annual savings / annual income × 100
     */
    public double savingsRate(FireInputDTO input) {
        double annualSavings = input.getMonthlySavings() * 12;
        return (annualSavings / input.getAnnualIncome()) * 100;
    }

    /**
     * Annual withdrawal available at retirement under the SWR applied
     * to the projected corpus.
     */
    public double annualWithdrawalAtRetirement(FireInputDTO input) {
        return projectedCorpusAtRetirement(input) * effectiveWithdrawalRate(input);
    }

    /**
     * Is the user on track? True if projected corpus ≥ inflation-adjusted FIRE number.
     */
    public boolean isOnTrack(FireInputDTO input) {
        return projectedCorpusAtRetirement(input) >= calculateInflationAdjustedFireNumber(input);
    }

    /**
     * Determine the year in which the user's corpus will first reach the
     * inflation-adjusted FIRE number (year-by-year simulation).
     *
     * Returns null if FIRE is not achieved within a 60-year horizon.
     */
    public Integer projectedFireYear(FireInputDTO input) {
        int currentYear = LocalDate.now().getYear();
        double corpus = input.getExistingCorpus();
        double monthlyRate = effectiveReturnRate(input) / 12;
        double pmt = input.getMonthlySavings();
        double inflation = effectiveInflationRate(input);
        double fireToday = calculateFireNumber(input);

        for (int year = 1; year <= 60; year++) {
            // Grow corpus for the year
            corpus = corpus * (1 + effectiveReturnRate(input))
                    + pmt * 12 * (1 + monthlyRate * 6); // simplified mid-year convention

            // Calculate inflation-adjusted FIRE target for this year
            double fireTarget = fireToday * Math.pow(1 + inflation, year);

            if (corpus >= fireTarget) {
                return currentYear + year;
            }
        }
        return null; // FIRE not achieved within 60 years
    }

    /**
     * Year-by-year projection timeline from now to retirement.
     */
    public List<ProjectionYearDTO> buildProjectionTimeline(FireInputDTO input) {
        List<ProjectionYearDTO> timeline = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();
        int yearsToRetirement = input.getTargetRetirementAge() - input.getCurrentAge();
        double corpus = input.getExistingCorpus();
        double annualReturn = effectiveReturnRate(input);
        double annualSavings = input.getMonthlySavings() * 12;
        double inflation = effectiveInflationRate(input);
        double fireToday = calculateFireNumber(input);

        for (int y = 1; y <= yearsToRetirement; y++) {
            double corpusStart = corpus;
            double returnsEarned = corpusStart * annualReturn;
            double corpusEnd = corpusStart + returnsEarned + annualSavings;
            double fireTarget = fireToday * Math.pow(1 + inflation, y);
            double pctAchieved = Math.min(100.0, (corpusEnd / fireTarget) * 100);

            timeline.add(ProjectionYearDTO.builder()
                    .year(currentYear + y)
                    .age(input.getCurrentAge() + y)
                    .corpusStart(round2(corpusStart))
                    .returnsEarned(round2(returnsEarned))
                    .savingsContributed(round2(annualSavings))
                    .corpusEnd(round2(corpusEnd))
                    .fireTarget(round2(fireTarget))
                    .percentageAchieved(round2(pctAchieved))
                    .build());

            corpus = corpusEnd;
            // Optional: model SIP step-up of ~10% per year for realism
            // annualSavings *= 1.10;
        }

        return timeline;
    }

    /**
     * Determine FIRE status based on how far behind/ahead the user is.
     */
    public com.fire.calculator.dto.FireStatus determineFireStatus(FireInputDTO input) {
        double projCorpus = projectedCorpusAtRetirement(input);
        double fireTarget = calculateInflationAdjustedFireNumber(input);

        if (input.getExistingCorpus() >= fireTarget) {
            return FireStatus.ALREADY_ACHIEVED;
        }

        double percentageAchieved = (projCorpus / fireTarget) * 100;

        if (percentageAchieved >= 90) return FireStatus.ON_TRACK;
        if (percentageAchieved >= 70) return FireStatus.SLIGHTLY_BEHIND;
        if (percentageAchieved >= 40) return FireStatus.SIGNIFICANTLY_BEHIND;
        return FireStatus.CRITICAL;
    }

    /**
     * Format a number as Indian currency string.
     * e.g. 2500000 → "₹25.00 Lakh", 25000000 → "₹2.50 Crore"
     */
    public String formatIndianCurrency(double amount, String currency) {
        String symbol = "INR".equalsIgnoreCase(currency) ? "₹" : currency + " ";
        if (amount >= 10_000_000) {
            return String.format("%s%.2f Crore", symbol, amount / 10_000_000);
        } else if (amount >= 100_000) {
            return String.format("%s%.2f Lakh", symbol, amount / 100_000);
        } else {
            return String.format("%s%.2f", symbol, amount);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
