package com.fire.calculator.dto;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FireResultDTO {
    private String name;
    private Double fireNumber;
    private String fireNumberFormatted;
    private Double corpusShortfall;
    private Double projectedCorpusAtRetirement;
    private Boolean isOnTrack;
    private Integer yearsToRetirement;
    private Integer projectedFireYear;
    private Integer fireYearDelta;
    private Double savingsRate;
    private Double requiredMonthlySip;
    private Double monthlySipDelta;
    private Double inflationAdjustedFireNumber;
    private Double annualWithdrawalAtRetirement;
    private List<ProjectionYearDTO> projectionTimeline;
    private FinancialSnapshotDTO financialSnapshot;
    private AiInsightsDTO aiInsights;
    private CalculationMetaDTO meta;
}
