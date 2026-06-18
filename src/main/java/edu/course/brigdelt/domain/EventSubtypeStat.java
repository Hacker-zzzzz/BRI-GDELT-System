package edu.course.brigdelt.domain;

public record EventSubtypeStat(
        String rootCode,
        String label,
        int eventCount
) {
    public String displayLabel() {
        return rootCode + " " + label;
    }
}
