package com.smartway.api.port.out;

import com.smartway.api.domain.enums.TrafficLevel;

public interface TrafficPort {

    /** Resultado de uma unica consulta de rota: transito + distancia. */
    record RouteInfo(TrafficLevel trafficLevel, double distanceKm, boolean estimated) {}

    RouteInfo getRouteInfo(String origin, String destination);

    default TrafficLevel getTrafficLevel(String origin, String destination) {
        return getRouteInfo(origin, destination).trafficLevel();
    }
}
