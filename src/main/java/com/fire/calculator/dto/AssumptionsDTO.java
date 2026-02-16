package com.fire.calculator.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssumptionsDTO {
    private Double withdrawalRate;
    private Double inflationRate;
    private Double expectedReturnRate;
    private String currency;
}
