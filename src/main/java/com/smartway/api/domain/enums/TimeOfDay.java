package com.smartway.api.domain.enums;

public enum TimeOfDay {
    MORNING,    // 6h - 12h
    AFTERNOON,  // 12h - 18h
    NIGHT,      // 18h - 0h
    LATE_NIGHT; // 0h - 6h

    public static TimeOfDay fromHour(int hour) {
        if (hour >= 6 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 18) return AFTERNOON;
        if (hour >= 18) return NIGHT;
        return LATE_NIGHT;
    }
}
