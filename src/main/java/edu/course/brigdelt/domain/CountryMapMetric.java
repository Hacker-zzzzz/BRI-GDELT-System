package edu.course.brigdelt.domain;

/**
 * 国家专题地图指标。
 *
 * <p>包含国家中心点和合作强度，用于按国家绘制地图热度或摘要信息。</p>
 */
public record CountryMapMetric(
        String countryCode,
        String countryName,
        String region,
        double latitude,
        double longitude,
        int totalEvents,
        int cooperationEvents,
        double cooperationIndex
) {
}
