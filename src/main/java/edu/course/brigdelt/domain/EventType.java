package edu.course.brigdelt.domain;

/**
 * 基于 GDELT CAMEO 根代码归并出的粗粒度事件类型。
 *
 * <p>课程展示中只区分合作、冲突和其他三类，便于后续计算合作指数和风险指数。</p>
 */
public enum EventType {
    COOPERATION,
    CONFLICT,
    OTHER;

    /**
     * 将 CAMEO 根代码映射为系统内部事件类型。
     */
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
