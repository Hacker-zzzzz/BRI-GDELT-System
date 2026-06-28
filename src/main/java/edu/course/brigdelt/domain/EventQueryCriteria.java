package edu.course.brigdelt.domain;

import java.time.LocalDate;

/**
 * GDELT 事件检索条件。
 *
 * <p>支持按日期、任一参与国家、Actor1/Actor2、区域和事件类型过滤，
 * limit 用于限制课堂演示时的表格返回量。</p>
 */
public record EventQueryCriteria(
        LocalDate startDate,
        LocalDate endDate,
        String anyCountryCode,
        String actor1CountryCode,
        String actor2CountryCode,
        String region,
        EventType eventType,
        int limit
) {
    public static final int DEFAULT_LIMIT = 500;
    public static final int MAX_LIMIT = 5_000;
}
