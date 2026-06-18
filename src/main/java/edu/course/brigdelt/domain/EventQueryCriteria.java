package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * Search conditions for GDELT event queries.
 */
public record EventQueryCriteria(
        LocalDate startDate,
        LocalDate endDate,
        String anyCountryCode,
        String actor1CountryCode,
        String actor2CountryCode,
        String region,
        EventType eventType,
        int limit
) {
    public static final int DEFAULT_LIMIT = 500;
    public static final int MAX_LIMIT = 5_000;
}
