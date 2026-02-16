package com.fire.calculator.dto;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiInsightsDTO {
    private String executiveSummary;
    private String detailedAnalysis;
    private List<String> actionItems;
    private String investmentStrategy;
    private List<String> riskFactors;
    private String motivationalNote;
    private String modelUsed;
}
