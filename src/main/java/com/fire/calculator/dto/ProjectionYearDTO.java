package com.fire.calculator.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectionYearDTO {
    private Integer year;
    private Integer age;
    private Double corpusStart;
    private Double returnsEarned;
    private Double savingsContributed;
    private Double corpusEnd;
    private Double fireTarget;
    private Double percentageAchieved;
}
