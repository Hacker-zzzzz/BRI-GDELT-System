package edu.course.brigdelt.domain;

/**
 * 风险热点变化结果。
 *
 * <p>对比相邻月份的风险指数和冲突事件增量，用于发现近期风险上升的国家。</p>
 */
public record RiskHotspot(
        String countryCode,
        String countryName,
        String region,
        String previousMonth,
        String currentMonth,
        double previousIndex,
        double currentIndex,
        double growth,
        int conflictEventIncrease
) {
}
