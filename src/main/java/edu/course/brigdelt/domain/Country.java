package edu.course.brigdelt.domain;

/**
 * Country metadata used to match GDELT CAMEO country codes.
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
