package com.smartway.api.domain.model;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DecisionResult {
    private ScoredOption recommended;
    private List<ScoredOption> allOptions;
    private String headline;
    private String explanation;
    private List<String> reasons;
    private List<Map<String, Object>> alternatives;
    private Map<String, Double> weightsUsed;
    private boolean aiGenerated;
}
