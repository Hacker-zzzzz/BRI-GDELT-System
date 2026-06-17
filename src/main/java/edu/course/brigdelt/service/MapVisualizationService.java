package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.GeoEventPoint;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.util.List;

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
}
