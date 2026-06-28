package edu.course.brigdelt.domain;

/**
 * 国家聚类分析结果。
 *
 * <p>结合合作指数、风险指数、冲突占比、Goldstein、AvgTone 和事件规模，
 * 给国家打上可解释的类型标签，便于答辩展示区域差异。</p>
 */
public record CountryClusterResult(
        String countryCode,
        String countryName,
        String region,
        int totalEvents,
        double cooperationIndex,
        double riskIndex,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        String clusterLabel,
        String explanation
) {
}
