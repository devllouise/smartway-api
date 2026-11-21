package com.smartway.api.port.out;

import com.smartway.api.domain.enums.TrafficLevel;

public interface TrafficPort {

    /** Resultado de uma unica consulta de rota: transito + distancia. */
    /** tollBrl: pedagio estimado da rota de carro em reais (0 quando nao ha pedagio ou o Google nao informa). */
    record RouteInfo(TrafficLevel trafficLevel, double distanceKm, boolean estimated,
                     double originLat, double originLng, double tollBrl) {
        public RouteInfo(TrafficLevel t, double d, boolean e, double lat, double lng) { this(t, d, e, lat, lng, 0.0); }
        // Sem coordenada real: usa o centro de Praia Grande
        public RouteInfo(TrafficLevel t, double d, boolean e) { this(t, d, e, -24.0058, -46.4028, 0.0); }
    }

    RouteInfo getRouteInfo(String origin, String destination);
}
