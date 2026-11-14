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
    private List<TransportMode> penalizedModes;
    private List<TransportMode> boostedModes;
    private double penaltyFactor;
    private double boostFactor;
    private boolean hardRestriction;
    /** Texto curto mostrado no card das opcoes afetadas (ex: LEI SECA) */
    private String tag;
    /** Cor da tag no app: DANGER, WARNING, WEATHER, INFO, POSITIVE */
    private String tagType;

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