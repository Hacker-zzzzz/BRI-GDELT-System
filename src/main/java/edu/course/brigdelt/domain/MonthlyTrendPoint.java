package edu.course.brigdelt.domain;

/**
 * Monthly bilateral trend metrics for table and chart presentation.
 */
public record MonthlyTrendPoint(
        String month,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        double cooperationIndex
) {
}
