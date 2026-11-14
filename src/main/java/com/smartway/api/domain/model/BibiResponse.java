package com.smartway.api.domain.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BibiResponse {
    private String headline;
    private String explanation;
    private boolean aiGenerated;
}
