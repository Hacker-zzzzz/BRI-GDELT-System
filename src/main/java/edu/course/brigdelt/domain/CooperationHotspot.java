package edu.course.brigdelt.domain;

/**
 * 合作热点变化结果。
 *
 * <p>对比相邻月份的合作指数和合作事件增量，用于发现近期合作升温的国家。</p>
 */
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
