package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.TrafficLevel;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrafficInfo {
    private TrafficLevel level;
    private int delayMinutes;
    private String description;
    private boolean available;
}
