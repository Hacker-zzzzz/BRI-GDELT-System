package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventSubtypeStat;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件检索服务。
 *
 * <p>负责规范化查询条件、限制返回规模、缓存查询结果，并将实际 SQL 查询委托给 repository。</p>
 */
public class EventQueryService {

    private static final int MAX_CACHE_ENTRIES = 64;
    private static final Map<SearchCacheKey, List<EventQueryResult>> SEARCH_CACHE = lruCache();
    private static final Map<CountCacheKey, Integer> COUNT_CACHE = lruCache();
    private static final Map<SubtypeCacheKey, List<EventSubtypeStat>> SUBTYPE_CACHE = lruCache();

    private final GdeltEventRepository eventRepository;
    private final ImportBatchRepository importBatchRepository;

    public EventQueryService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public EventQueryService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
        this.importBatchRepository = new ImportBatchRepository(databaseManager);
    }

    /**
     * 执行多条件事件检索，结果用于事件查询表格。
     */
    public List<EventQueryResult> search(EventQueryCriteria criteria) {
        EventQueryCriteria normalized = normalize(criteria);
        SearchCacheKey key = new SearchCacheKey(cacheVersion(), normalized);
        synchronized (SEARCH_CACHE) {
            List<EventQueryResult> cached = SEARCH_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<EventQueryResult> results = eventRepository.queryEvents(normalized);
        synchronized (SEARCH_CACHE) {
            SEARCH_CACHE.put(key, results);
        }
        return results;
    }

    /**
     * 统计检索命中总数，和表格结果分开缓存以便界面快速显示规模。
     */
    public int count(EventQueryCriteria criteria) {
        EventQueryCriteria normalized = normalize(criteria);
        CountCacheKey key = new CountCacheKey(cacheVersion(), normalized);
        synchronized (COUNT_CACHE) {
            Integer cached = COUNT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        int count = eventRepository.countEvents(normalized);
        synchronized (COUNT_CACHE) {
            COUNT_CACHE.put(key, count);
        }
        return count;
    }

    /**
     * 查询事件根代码分布，用于解释合作/冲突内部结构。
     */
    public List<EventSubtypeStat> subtypeDistribution(EventQueryCriteria criteria, EventType eventType) {
        EventQueryCriteria normalized = normalize(criteria);
        SubtypeCacheKey key = new SubtypeCacheKey(cacheVersion(), normalized, eventType);
        synchronized (SUBTYPE_CACHE) {
            List<EventSubtypeStat> cached = SUBTYPE_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        Map<String, Integer> countsByCode = new LinkedHashMap<>();
        for (EventSubtypeStat stat : eventRepository.queryEventSubtypeStats(normalized, eventType)) {
            countsByCode.put(stat.rootCode(), stat.eventCount());
        }
        List<EventSubtypeStat> results = subtypeLabels(eventType).entrySet().stream()
                .map(entry -> new EventSubtypeStat(
                        entry.getKey(),
                        entry.getValue(),
                        countsByCode.getOrDefault(entry.getKey(), 0)
                ))
                .toList();
        synchronized (SUBTYPE_CACHE) {
            SUBTYPE_CACHE.put(key, results);
        }
        return results;
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

    private CacheVersion cacheVersion() {
        return new CacheVersion(eventRepository.countEvents(), importBatchRepository.countImportBatches());
    }

    private static <K, V> Map<K, V> lruCache() {
        return Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        });
    }

    private record CacheVersion(int eventCount, int importBatchCount) {
    }

    private record SearchCacheKey(CacheVersion version, EventQueryCriteria criteria) {
    }

    private record CountCacheKey(CacheVersion version, EventQueryCriteria criteria) {
    }

    private record SubtypeCacheKey(CacheVersion version, EventQueryCriteria criteria, EventType eventType) {
    }
}
