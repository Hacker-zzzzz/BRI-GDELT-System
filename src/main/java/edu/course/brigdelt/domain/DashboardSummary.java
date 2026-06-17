package edu.course.brigdelt.domain;

/**
 * High-level metrics for the home dashboard.
 */
public record DashboardSummary(
        int countryCount,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        int otherEvents,
        int importBatches,
        int totalMentions,
        double averageGoldstein,
        double averageAvgTone
) {
}
