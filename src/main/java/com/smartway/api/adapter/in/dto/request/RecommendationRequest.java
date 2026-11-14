package com.smartway.api.adapter.in.dto.request;

import com.smartway.api.domain.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    private String origin;
    private String destination;
    private String originAddress;
    private double originLat;
    private double originLng;
    private String destinationAddress;
    private double destinationLat;
    private double destinationLng;
    private String intent;
    private String priority;
    private List<TransportMode> preferredModes;

    public String getOrigin() {
        return origin != null ? origin : originAddress;
    }

    public String getDestination() {
        return destination != null ? destination : destinationAddress;
    }
}