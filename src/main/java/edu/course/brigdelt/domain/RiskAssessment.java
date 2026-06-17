package edu.course.brigdelt.domain;

public record RiskAssessment(
        String countryCode,
        int totalEvents,
        int conflictEvents,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        double riskIndex,
        String riskLevel
) {
}
