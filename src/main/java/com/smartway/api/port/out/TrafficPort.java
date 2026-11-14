package com.smartway.api.port.out;

import com.smartway.api.domain.enums.TrafficLevel;

public interface TrafficPort {

    /** Resultado de uma unica consulta de rota: transito + distancia. */
    record RouteInfo(TrafficLevel trafficLevel, double distanceKm, boolean estimated,
                     double originLat, double originLng) {
        // Sem coordenada real: usa o centro de Praia Grande
        public RouteInfo(TrafficLevel t, double d, boolean e) { this(t, d, e, -24.0058, -46.4028); }
    }

    RouteInfo getRouteInfo(String origin, String destination);
}
