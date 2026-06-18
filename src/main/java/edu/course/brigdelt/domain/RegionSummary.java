package edu.course.brigdelt.domain;

public record RegionSummary(
        String region,
        int countryCount,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions,
        double cooperationIndex,
        double riskIndex
) {
}
