package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.Country;
import edu.course.brigdelt.domain.CountryMapMetric;
import edu.course.brigdelt.domain.GeoEventPoint;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapVisualizationService {

    public static final int DEFAULT_POINT_LIMIT = 500;

    private final GdeltEventRepository eventRepository;
    private final CountryRepository countryRepository;

    public MapVisualizationService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
        this.countryRepository = new CountryRepository(databaseManager);
    }

    public List<GeoEventPoint> geoEventPoints(int limit) {
        if (limit <= 0) {
            return eventRepository.queryGeoEventPoints(DEFAULT_POINT_LIMIT);
        }
        return eventRepository.queryGeoEventPoints(Math.min(limit, 2000));
    }

    public List<GeoEventPoint> geoEventPoints(int limit, Set<String> eventTypes, String countryCode) {
        int effectiveLimit = limit <= 0 ? DEFAULT_POINT_LIMIT : Math.min(limit, 2000);
        return eventRepository.queryGeoEventPoints(effectiveLimit, eventTypes, countryCode);
    }

    public List<LocalDate> geoEventDates(Set<String> eventTypes, String countryCode) {
        return eventRepository.queryGeoEventDates(eventTypes, countryCode);
    }

    public List<GeoEventPoint> geoEventPoints(int limit, Set<String> eventTypes, String countryCode,
                                              LocalDate startDate, LocalDate endDate) {
        int effectiveLimit = limit <= 0 ? DEFAULT_POINT_LIMIT : Math.min(limit, 2000);
        return eventRepository.queryGeoEventPoints(effectiveLimit, eventTypes, countryCode, startDate, endDate);
    }

    public Map<LocalDate, List<GeoEventPoint>> geoEventPointSeries(int limit, Set<String> eventTypes, String countryCode) {
        int effectiveLimit = limit <= 0 ? DEFAULT_POINT_LIMIT : Math.min(limit, 2000);
        return eventRepository.queryGeoEventPointSeries(effectiveLimit, eventTypes, countryCode);
    }

    public List<CountryMapMetric> countryCooperationMetrics() {
        Map<String, CooperationScore> scoresByCountry = eventRepository.queryCooperationScores(1_000).stream()
                .collect(Collectors.toMap(CooperationScore::countryCode, Function.identity(), (left, right) -> left));
        return countryRepository.findAllCountries().stream()
                .filter(Country::briCountry)
                .filter(country -> country.latitude() != null && country.longitude() != null)
                .map(country -> {
                    CooperationScore score = scoresByCountry.get(country.cameoCode());
                    return new CountryMapMetric(
                            country.cameoCode(),
                            country.nameCn() == null || country.nameCn().isBlank() ? country.nameEn() : country.nameCn(),
                            country.region(),
                            country.latitude(),
                            country.longitude(),
                            score == null ? 0 : score.totalEvents(),
                            score == null ? 0 : score.cooperationEvents(),
                            score == null ? 0 : score.cooperationIndex()
                    );
                })
                .sorted(Comparator.comparingInt(CountryMapMetric::cooperationEvents).reversed()
                        .thenComparing(Comparator.comparingDouble(CountryMapMetric::cooperationIndex).reversed()))
                .toList();
    }
}
