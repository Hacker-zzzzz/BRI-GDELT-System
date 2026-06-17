package edu.course.brigdelt.domain;

public record CooperationScore(
        String countryCode,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions,
        double cooperationIndex
) {
}
