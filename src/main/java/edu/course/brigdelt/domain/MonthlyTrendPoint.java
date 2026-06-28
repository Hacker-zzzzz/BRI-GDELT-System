package edu.course.brigdelt.domain;

/**
 * 月度趋势点。
 *
 * <p>用于折线图和趋势表格，展示某个分析对象在月份维度上的事件数量、
 * 合作/冲突结构和合作指数变化。</p>
 */
public record MonthlyTrendPoint(
        String month,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        double cooperationIndex
) {
}
