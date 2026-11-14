package com.smartway.api.domain.model;

import java.util.List;

/** Rota real de transporte publico (Google Routes, modo TRANSIT). */
public record TransitRoute(int minutes, double fare, boolean fareKnown, double distanceKm, List<Leg> legs) {

    /** Trecho: WALK (caminhada) ou BUS (com a linha). */
    public record Leg(String type, String line, int minutes) {}

    public int walkMinutes() {
        return legs.stream().filter(l -> "WALK".equals(l.type())).mapToInt(Leg::minutes).sum();
    }

    public long busLegs() {
        return legs.stream().filter(l -> "BUS".equals(l.type())).count();
    }
}
