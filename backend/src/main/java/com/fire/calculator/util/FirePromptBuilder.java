package com.fire.calculator.util;

import com.fire.calculator.dto.FireInputDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Builds structured, context-rich prompts for the Groq LLM.
 *
 * Design principles:
 *  - Role-based system prompt (certified financial planner persona)
 *  - All computed FIRE metrics injected into context (not re-derived by AI)
 *  - JSON-structured output requested for reliable parsing
 *  - Indian finance context emphasized (PPF, NPS, EPF, Nifty 50, etc.)
 *  - Temperature kept low (0.2) for deterministic, accurate financial advice
 */
@Component
public class FirePromptBuilder {

    @Value("${fire.calculator.ai.system-prompt-context}")
    private String systemPromptContext;

    /**
     * Build the SYSTEM prompt (role and behavioral instructions for the LLM).
     */
    public String buildSystemPrompt() {
        return systemPromptContext + """

You must respond ONLY with a valid JSON object in the following exact structure.
Do not include any markdown, explanation, or text outside the JSON.

{
  "executiveSummary": "<2-3 sentence concise FIRE readiness summary>",
  "detailedAnalysis": "<400-500 word detailed analysis of their financial situation, trajectory, gaps, and opportunities>",
  "actionItems": [
    "<Specific action item 1 with numbers and timeline>",
    "<Specific action item 2 with numbers and timeline>",
    "<Specific action item 3>",
    "<Specific action item 4>",
    "<Specific action item 5>"
  ],
  "investmentStrategy": "<200-300 word investment allocation strategy recommendation based on risk profile, age, and Indian market instruments like Nifty 50 index funds, PPF, NPS, debt funds, etc.>",
  "riskFactors": [
    "<Risk factor 1 specific to their profile>",
    "<Risk factor 2>",
    "<Risk factor 3>"
  ],
  "motivationalNote": "<1-2 inspiring, realistic sentences about their FIRE journey>"
}
""";
    }

    /**
     * Build the USER prompt with all computed FIRE data injected as context.
     * The LLM is NOT asked to do math — only to interpret and advise.
     */
    public String buildUserPrompt(
            FireInputDTO input,
            double fireNumber,
            double inflationAdjustedFireNumber,
            double projectedCorpus,
            double corpusShortfall,
            double requiredMonthlySip,
            double monthlySipDelta,
            double savingsRate,
            double annualWithdrawal,
            boolean isOnTrack,
            int yearsToRetirement,
            Integer projectedFireYear,
            String currency
    ) {
        String additionalExpensesStr = (input.getAdditionalExpenses() != null && !input.getAdditionalExpenses().isEmpty())
                ? input.getAdditionalExpenses().stream()
                .map(e -> "  - " + e.getLabel() + ": ₹" + formatAmount(e.getAnnualAmount()) + "/year")
                .collect(Collectors.joining("\n"))
                : "  None specified";

        String riskProfileStr = input.getRiskProfile() != null
                ? input.getRiskProfile().name()
                : "MODERATE (assumed)";

        String goalStr = (input.getRetirementGoal() != null && !input.getRetirementGoal().isBlank())
                ? input.getRetirementGoal()
                : "Standard comfortable retirement";

        boolean detailedAnalysis = Boolean.TRUE.equals(input.getDetailedAnalysis());

        return """
FIRE ANALYSIS REQUEST
=====================

PERSONAL PROFILE:
  Name: %s
  Current Age: %d years
  Target Retirement Age: %d years
  Years to Retirement: %d years
  Risk Profile: %s
  Retirement Goal: %s
  Currency: %s

INCOME & EXPENSE SNAPSHOT:
  Annual Income: ₹%s
  Annual Expenses (Base): ₹%s
  Additional Goal Expenses:
%s
  Monthly SIP / Savings: ₹%s
  Annual Savings: ₹%s
  Savings Rate: %.1f%% of income

COMPUTED FIRE METRICS (ALREADY CALCULATED — DO NOT RECALCULATE):
  FIRE Number (today's money): ₹%s
  Inflation-Adjusted FIRE Number (at retirement): ₹%s
  Projected Corpus at Retirement: ₹%s
  Corpus Shortfall / Surplus: ₹%s (%s)
  Currently On Track: %s
  Required Monthly SIP: ₹%s
  Current Monthly SIP: ₹%s
  Monthly SIP Surplus/Deficit: ₹%s (%s)
  Annual Withdrawal at Retirement (under %.0f%% SWR): ₹%s
  Projected FIRE Achievement Year: %s

ASSUMPTIONS USED:
  Expected Annual Return: %.1f%%
  Annual Inflation Rate: %.1f%%
  Safe Withdrawal Rate: %.1f%%

ANALYSIS DEPTH REQUESTED: %s

Please provide comprehensive, India-specific financial advice based on the above data.
Focus on practical, actionable recommendations using Indian investment instruments.
""".formatted(
                input.getName(),
                input.getCurrentAge(),
                input.getTargetRetirementAge(),
                yearsToRetirement,
                riskProfileStr,
                goalStr,
                currency,
                formatAmount(input.getAnnualIncome()),
                formatAmount(input.getAnnualExpenses()),
                additionalExpensesStr,
                formatAmount(input.getMonthlySavings()),
                formatAmount(input.getMonthlySavings() * 12),
                savingsRate,
                formatAmount(fireNumber),
                formatAmount(inflationAdjustedFireNumber),
                formatAmount(projectedCorpus),
                formatAmount(Math.abs(corpusShortfall)),
                corpusShortfall > 0 ? "SHORTFALL" : "SURPLUS",
                isOnTrack ? "YES ✓" : "NO ✗",
                formatAmount(requiredMonthlySip),
                formatAmount(input.getMonthlySavings()),
                formatAmount(Math.abs(monthlySipDelta)),
                monthlySipDelta >= 0 ? "SURPLUS" : "DEFICIT",
                (input.getWithdrawalRate() != null ? input.getWithdrawalRate() : 0.04) * 100,
                formatAmount(annualWithdrawal),
                projectedFireYear != null ? projectedFireYear.toString() : "Not achievable in 60-year window",
                (input.getExpectedReturnRate() != null ? input.getExpectedReturnRate() : 0.12) * 100,
                (input.getInflationRate() != null ? input.getInflationRate() : 0.06) * 100,
                (input.getWithdrawalRate() != null ? input.getWithdrawalRate() : 0.04) * 100,
                detailedAnalysis ? "DETAILED (full analysis requested)" : "BRIEF (summary only)"
        );
    }

    private String formatAmount(double amount) {
        if (amount >= 10_000_000) {
            return String.format("%.2f Crore", amount / 10_000_000);
        } else if (amount >= 100_000) {
            return String.format("%.2f Lakh", amount / 100_000);
        }
        return String.format("%.2f", amount);
    }
}
