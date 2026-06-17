package edu.course.brigdelt.domain;

/**
 * Event count grouped by country code.
 */
public record CountryEventStat(
        String countryCode,
        int eventCount
) {
}
