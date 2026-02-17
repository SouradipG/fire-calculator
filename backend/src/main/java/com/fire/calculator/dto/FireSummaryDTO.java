package com.fire.calculator.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FireSummaryDTO {
    private Double fireNumber;
    private Boolean isOnTrack;
    private Integer yearsToRetirement;
    private Double requiredMonthlySip;
    private FireStatus status;
    private String summary;
}
