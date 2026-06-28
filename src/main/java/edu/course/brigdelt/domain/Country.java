package edu.course.brigdelt.domain;

/**
 * 国家基础配置，用于把 GDELT/CAMEO 国家代码映射到中文名称、区域和地图坐标。
 */
public record Country(
        String cameoCode,
        String isoCode,
        String nameCn,
        String nameEn,
        String region,
        Double latitude,
        Double longitude,
        boolean briCountry,
        boolean coreCountry
) {
}
