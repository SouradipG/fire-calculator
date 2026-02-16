package com.fire.calculator.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CalculationMetaDTO {
    private String calculatedAt;
    private AssumptionsDTO assumptions;
}
