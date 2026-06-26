package edu.course.brigdelt.domain;

public record CooperationScore(
        String countryCode,
        String countryName,
        String region,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions,
        double cooperationIndex
) {
}
