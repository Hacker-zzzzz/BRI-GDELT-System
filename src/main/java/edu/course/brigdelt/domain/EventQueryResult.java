package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * Projection used by the event query table.
 */
public record EventQueryResult(
        String globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        EventType eventType,
        String eventRootCode,
        double goldsteinScale,
        int numMentions,
        double avgTone,
        String sourceFile
) {
}
