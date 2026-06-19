package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides validated bilateral relation analysis queries.
 */
public class BilateralRelationService {

    public static final String DEFAULT_COUNTRY_A = "CHN";
    public static final int DEFAULT_EVENT_LIMIT = 200;
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final Map<PairCacheKey, BilateralRelationSummary> SUMMARY_CACHE = lruCache();
    private static final Map<PairEventsCacheKey, List<EventQueryResult>> EVENTS_CACHE = lruCache();
    private static final Map<PairCacheKey, List<MonthlyTrendPoint>> TREND_CACHE = lruCache();

    private final GdeltEventRepository eventRepository;
    private final ImportBatchRepository importBatchRepository;

    public BilateralRelationService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public BilateralRelationService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
        this.importBatchRepository = new ImportBatchRepository(databaseManager);
    }

    public BilateralRelationSummary summarize(String countryA, String countryB) {
        CountryPair pair = normalizePair(countryA, countryB);
        PairCacheKey key = new PairCacheKey(cacheVersion(), pair);
        synchronized (SUMMARY_CACHE) {
            BilateralRelationSummary cached = SUMMARY_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        BilateralRelationSummary summary = eventRepository.summarizeBilateral(pair.countryA(), pair.countryB());
        synchronized (SUMMARY_CACHE) {
            SUMMARY_CACHE.put(key, summary);
        }
        return summary;
    }

    public List<EventQueryResult> events(String countryA, String countryB, int limit) {
        CountryPair pair = normalizePair(countryA, countryB);
        int safeLimit = limit <= 0 ? DEFAULT_EVENT_LIMIT : Math.min(limit, 1_000);
        PairEventsCacheKey key = new PairEventsCacheKey(cacheVersion(), pair, safeLimit);
        synchronized (EVENTS_CACHE) {
            List<EventQueryResult> cached = EVENTS_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<EventQueryResult> results = eventRepository.queryBilateralEvents(pair.countryA(), pair.countryB(), safeLimit);
        synchronized (EVENTS_CACHE) {
            EVENTS_CACHE.put(key, results);
        }
        return results;
    }

    public List<MonthlyTrendPoint> monthlyTrend(String countryA, String countryB) {
        CountryPair pair = normalizePair(countryA, countryB);
        PairCacheKey key = new PairCacheKey(cacheVersion(), pair);
        synchronized (TREND_CACHE) {
            List<MonthlyTrendPoint> cached = TREND_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<MonthlyTrendPoint> results = eventRepository.queryBilateralMonthlyTrend(pair.countryA(), pair.countryB());
        synchronized (TREND_CACHE) {
            TREND_CACHE.put(key, results);
        }
        return results;
    }

    private CountryPair normalizePair(String countryA, String countryB) {
        String safeA = normalizeCode(countryA);
        String safeB = normalizeCode(countryB);
        if (safeA == null) {
            safeA = DEFAULT_COUNTRY_A;
        }
        if (safeB == null) {
            throw new IllegalArgumentException("国家 B 不能为空。");
        }
        if (safeA.equals(safeB)) {
            throw new IllegalArgumentException("国家 A 和国家 B 不能相同。");
        }
        return new CountryPair(safeA, safeB);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
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

    private record PairCacheKey(CacheVersion version, CountryPair pair) {
    }

    private record PairEventsCacheKey(CacheVersion version, CountryPair pair, int limit) {
    }

    private record CountryPair(String countryA, String countryB) {
    }
}
