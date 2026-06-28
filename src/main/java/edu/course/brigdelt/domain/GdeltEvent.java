package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * 系统持久化的一条 GDELT 事件记录，保留事件主体、CAMEO 编码、情绪和地理坐标等核心字段。
 */
public record GdeltEvent(
        long globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        String eventCode,
        String eventBaseCode,
        String eventRootCode,
        EventType eventType,
        double goldsteinScale,
        int numMentions,
        double avgTone,
        Double actionGeoLat,
        Double actionGeoLon,
        String sourceFile
) {
}
