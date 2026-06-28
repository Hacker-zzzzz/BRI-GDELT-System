package edu.course.brigdelt.domain;

/**
 * 国家风险评估结果。
 *
 * <p>用于风险评估页面，riskIndex 综合冲突占比、冲突事件规模、
 * 负向 Goldstein 和负向 AvgTone，riskLevel 给出课堂展示用风险等级。</p>
 */
public record RiskAssessment(
        String countryCode,
        int totalEvents,
        int conflictEvents,
        double conflictRatio,
        double averageGoldstein,
        double averageAvgTone,
        double riskIndex,
        String riskLevel
) {
}
