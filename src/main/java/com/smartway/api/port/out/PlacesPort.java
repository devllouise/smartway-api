package com.smartway.api.port.out;

import com.smartway.api.domain.enums.DestinationType;

public interface PlacesPort {
    DestinationType getDestinationType(String destination);
}