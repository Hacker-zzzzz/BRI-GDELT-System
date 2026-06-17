package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Validates query options and delegates event lookup to the repository layer.
 */
public class EventQueryService {

    private final GdeltEventRepository eventRepository;

    public EventQueryService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public EventQueryService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
    }

    public List<EventQueryResult> search(EventQueryCriteria criteria) {
        return eventRepository.queryEvents(normalize(criteria));
    }

    public int count(EventQueryCriteria criteria) {
        return eventRepository.countEvents(normalize(criteria));
    }

    private EventQueryCriteria normalize(EventQueryCriteria criteria) {
        EventQueryCriteria safeCriteria = criteria == null
                ? new EventQueryCriteria(null, null, null, null, null, null, EventQueryCriteria.DEFAULT_LIMIT)
                : criteria;
        LocalDate startDate = safeCriteria.startDate();
        LocalDate endDate = safeCriteria.endDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期。");
        }
        int limit = safeCriteria.limit();
        if (limit <= 0) {
            limit = EventQueryCriteria.DEFAULT_LIMIT;
        } else if (limit > EventQueryCriteria.MAX_LIMIT) {
            limit = EventQueryCriteria.MAX_LIMIT;
        }
        return new EventQueryCriteria(
                startDate,
                endDate,
                normalizeCode(safeCriteria.anyCountryCode()),
                normalizeCode(safeCriteria.actor1CountryCode()),
                normalizeCode(safeCriteria.actor2CountryCode()),
                safeCriteria.eventType(),
                limit
        );
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase();
    }
}
