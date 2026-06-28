package edu.course.brigdelt.domain;

/**
 * 两国关系汇总指标，表示无方向国家对在当前筛选条件下的合作、冲突和情绪统计。
 */
public record BilateralRelationSummary(
        String countryA,
        String countryB,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        int otherEvents,
        double cooperationRatio,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions
) {
    public static BilateralRelationSummary empty(String countryA, String countryB) {
        return new BilateralRelationSummary(countryA, countryB, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
