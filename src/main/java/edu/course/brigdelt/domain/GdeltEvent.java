package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * Domain model for one GDELT event row persisted by the application.
 */
public record GdeltEvent(
        long globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        String eventCode,
        String eventBaseCode,
        String eventRootCode,
        EventType eventType,
        double goldsteinScale,
        int numMentions,
        double avgTone,
        Double actionGeoLat,
        Double actionGeoLon,
        String sourceFile
) {
}
