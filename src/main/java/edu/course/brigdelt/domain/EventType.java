package edu.course.brigdelt.domain;

/**
 * Coarse event type derived from the GDELT CAMEO event root code.
 */
public enum EventType {
    COOPERATION,
    CONFLICT,
    OTHER;

    public static EventType fromRootCode(String rootCode) {
        if (rootCode == null || rootCode.isBlank()) {
            return OTHER;
        }

        String normalized = rootCode.trim();
        return switch (normalized) {
            case "04", "05", "06" -> COOPERATION;
            case "08", "09", "10", "11", "12", "13", "14" -> CONFLICT;
            default -> OTHER;
        };
    }
}
