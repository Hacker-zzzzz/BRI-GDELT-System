package edu.course.brigdelt.domain;

/**
 * 区域汇总分析结果。
 *
 * <p>将沿线国家按子区域聚合，支持区域合作指数、风险指数和事件结构对比。</p>
 */
public record RegionSummary(
        String region,
        int countryCount,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions,
        double cooperationIndex,
        double riskIndex
) {
}
