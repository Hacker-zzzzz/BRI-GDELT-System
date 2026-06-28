package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * 可在专题地图上绘制的 GDELT 地理事件点。
 *
 * <p>经纬度来自 ActionGeo 字段，事件类型、Goldstein 和 AvgTone 用于地图着色、
 * 悬浮提示和空间分布研判。</p>
 */
public record GeoEventPoint(
        String globalEventId,
        LocalDate eventDate,
        String actor1CountryCode,
        String actor2CountryCode,
        EventType eventType,
        double latitude,
        double longitude,
        double goldsteinScale,
        double avgTone
) {
}
