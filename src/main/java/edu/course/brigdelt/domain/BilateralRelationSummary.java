package edu.course.brigdelt.domain;

/**
 * Aggregated metrics for an unordered country pair.
 */
public record BilateralRelationSummary(
        String countryA,
        String countryB,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        int otherEvents,
        double cooperationRatio,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions
) {
    public static BilateralRelationSummary empty(String countryA, String countryB) {
        return new BilateralRelationSummary(countryA, countryB, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
