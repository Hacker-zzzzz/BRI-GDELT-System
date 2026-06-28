package edu.course.brigdelt.domain;

/**
 * 首页仪表盘核心汇总指标。
 *
 * <p>用于快速展示国家配置数量、事件总量、合作/冲突结构、导入批次和总体语调。</p>
 */
public record DashboardSummary(
        int countryCount,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        int otherEvents,
        int importBatches,
        int totalMentions,
        double averageGoldstein,
        double averageAvgTone
) {
}
