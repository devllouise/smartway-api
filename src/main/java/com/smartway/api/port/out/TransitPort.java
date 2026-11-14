package com.smartway.api.port.out;

import com.smartway.api.domain.model.TransitRoute;

import java.util.Optional;

public interface TransitPort {
    /** transitPreference: FEWER_TRANSFERS, FASTER, CHEAPER, COMFORT (ou null). */
    Optional<TransitRoute> findRoute(String origin, String destination, String transitPreference);
}
