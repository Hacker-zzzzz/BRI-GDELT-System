package edu.course.brigdelt.domain;

/**
 * 国家合作评分结果。
 *
 * <p>用于合作态势页面和导出结果，综合事件数量、合作/冲突结构、
 * Goldstein、AvgTone 和媒体关注度形成 cooperationIndex。</p>
 */
public record CooperationScore(
        String countryCode,
        String countryName,
        String region,
        int totalEvents,
        int cooperationEvents,
        int conflictEvents,
        double averageGoldstein,
        double averageAvgTone,
        int totalMentions,
        double cooperationIndex
) {
}
