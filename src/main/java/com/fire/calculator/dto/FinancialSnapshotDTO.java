package com.fire.calculator.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialSnapshotDTO {
    private Double annualIncome;
    private Double annualExpenses;
    private Double annualSavings;
    private Double savingsRatePercent;
    private Double existingCorpus;
    private Double monthlySavings;
    private Double expectedReturnRatePercent;
    private Double inflationRatePercent;
    private Double withdrawalRatePercent;
}
