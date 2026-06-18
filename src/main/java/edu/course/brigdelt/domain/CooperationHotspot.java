package edu.course.brigdelt.domain;

public record CooperationHotspot(
        String countryCode,
        String countryName,
        String region,
        String previousMonth,
        String currentMonth,
        double previousIndex,
        double currentIndex,
        double growth,
        int cooperationEventIncrease
) {
}
