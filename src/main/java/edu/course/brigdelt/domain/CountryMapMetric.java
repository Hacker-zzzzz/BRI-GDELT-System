package edu.course.brigdelt.domain;

public record CountryMapMetric(
        String countryCode,
        String countryName,
        String region,
        double latitude,
        double longitude,
        int totalEvents,
        int cooperationEvents,
        double cooperationIndex
) {
}
