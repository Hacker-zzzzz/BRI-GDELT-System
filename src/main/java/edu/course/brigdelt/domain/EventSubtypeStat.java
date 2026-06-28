package edu.course.brigdelt.domain;

/**
 * CAMEO 根代码下的事件子类型统计，用于展示合作/冲突事件的结构分布。
 */
public record EventSubtypeStat(
        String rootCode,
        String label,
        int eventCount
) {
    public String displayLabel() {
        return rootCode + " " + label;
    }
}
