package edu.course.brigdelt.domain;

/**
 * 按国家汇总的事件数量，用于排行榜、下拉筛选和区域统计的基础数据。
 */
public record CountryEventStat(
        String countryCode,
        int eventCount
) {
}
