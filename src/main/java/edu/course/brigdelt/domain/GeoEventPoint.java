package edu.course.brigdelt.domain;

import java.time.LocalDate;

public record GeoEventPoint(
        String globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        EventType eventType,
        double latitude,
        double longitude,
        double goldsteinScale,
        double avgTone
) {
}
