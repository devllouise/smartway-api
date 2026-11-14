package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextInsight {

    private String id;
    private String severity;
    private String category;
    private String title;
    private String message;
    private TransportMode affectedMode;
    private List<TransportMode> penalizedModes;
    private List<TransportMode> boostedModes;
    private double penaltyFactor;
    private double boostFactor;
    private boolean hardRestriction;

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isWarning() {
        return "WARNING".equals(severity);
    }

    public boolean isInfo() {
        return "INFO".equals(severity);
    }
}