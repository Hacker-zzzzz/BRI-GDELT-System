package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * 事件查询表格展示用结果。
 *
 * <p>只保留界面检索和答辩说明需要的核心字段，避免把 GDELT 原始宽表全部暴露到 UI。</p>
 */
public record EventQueryResult(
        String globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        EventType eventType,
        String eventRootCode,
        double goldsteinScale,
        int numMentions,
        double avgTone,
        String sourceFile
) {
}
