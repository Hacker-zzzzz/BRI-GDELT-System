package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventSubtypeStat;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<EventSubtypeStat> subtypeDistribution(EventQueryCriteria criteria, EventType eventType) {
        EventQueryCriteria normalized = normalize(criteria);
        Map<String, Integer> countsByCode = new LinkedHashMap<>();
        for (EventSubtypeStat stat : eventRepository.queryEventSubtypeStats(normalized, eventType)) {
            countsByCode.put(stat.rootCode(), stat.eventCount());
        }
        return subtypeLabels(eventType).entrySet().stream()
                .map(entry -> new EventSubtypeStat(
                        entry.getKey(),
                        entry.getValue(),
                        countsByCode.getOrDefault(entry.getKey(), 0)
                ))
                .toList();
    }

    private EventQueryCriteria normalize(EventQueryCriteria criteria) {
        EventQueryCriteria safeCriteria = criteria == null
                ? new EventQueryCriteria(null, null, null, null, null, null, null, EventQueryCriteria.DEFAULT_LIMIT)
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
                normalizeRegion(safeCriteria.region()),
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

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        return region.trim().toUpperCase();
    }

    private Map<String, String> subtypeLabels(EventType eventType) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (eventType == EventType.COOPERATION) {
            labels.put("04", "咨询");
            labels.put("05", "外交合作");
            labels.put("06", "实质合作");
            return labels;
        }
        if (eventType == EventType.CONFLICT) {
            labels.put("08", "让步/屈服");
            labels.put("09", "调查");
            labels.put("10", "要求");
            labels.put("11", "不赞成");
            labels.put("12", "拒绝");
            labels.put("13", "威胁");
            labels.put("14", "抗议");
            return labels;
        }
        return labels;
    }
}
