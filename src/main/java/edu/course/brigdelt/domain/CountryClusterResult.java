package edu.course.brigdelt.domain;

public record CountryClusterResult(
        String countryCode,
        String countryName,
        String region,
        int totalEvents,
        double cooperationIndex,
        double riskIndex,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        String clusterLabel,
        String explanation
) {
}
