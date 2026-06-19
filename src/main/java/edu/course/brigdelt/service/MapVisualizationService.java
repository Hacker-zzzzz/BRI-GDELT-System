package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.GeoEventPoint;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapVisualizationService {

    public static final int DEFAULT_POINT_LIMIT = 500;

    private final GdeltEventRepository eventRepository;

    public MapVisualizationService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
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
}
