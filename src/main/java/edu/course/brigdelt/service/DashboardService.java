package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.CountryEventStat;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads aggregated values used by the dashboard page.
 */
public class DashboardService {

    private static final int MAX_CACHE_ENTRIES = 16;
    private static final Map<SummaryCacheKey, DashboardSummary> SUMMARY_CACHE = lruCache();
    private static final Map<TopCountriesCacheKey, List<CountryEventStat>> TOP_COUNTRIES_CACHE = lruCache();
    private static final Map<CacheVersion, List<MonthlyTrendPoint>> DAILY_TREND_CACHE = lruCache();

    private final CountryRepository countryRepository;
    private final GdeltEventRepository eventRepository;
    private final ImportBatchRepository importBatchRepository;

    public DashboardService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public DashboardService(DatabaseManager databaseManager) {
        this.countryRepository = new CountryRepository(databaseManager);
        this.eventRepository = new GdeltEventRepository(databaseManager);
        this.importBatchRepository = new ImportBatchRepository(databaseManager);
    }

    public DashboardSummary loadSummary() {
        int countryCount = countryRepository.countCountries();
        int importBatchCount = importBatchRepository.countImportBatches();
        SummaryCacheKey key = new SummaryCacheKey(new CacheVersion(eventRepository.countEvents(), importBatchCount),
                countryCount);
        synchronized (SUMMARY_CACHE) {
            DashboardSummary cached = SUMMARY_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        DashboardSummary summary = eventRepository.loadDashboardSummary(countryCount, importBatchCount);
        synchronized (SUMMARY_CACHE) {
            SUMMARY_CACHE.put(key, summary);
        }
        return summary;
    }

    public List<CountryEventStat> topCountries(int limit) {
        int safeLimit = limit <= 0 ? 8 : limit;
        TopCountriesCacheKey key = new TopCountriesCacheKey(cacheVersion(), safeLimit);
        synchronized (TOP_COUNTRIES_CACHE) {
            List<CountryEventStat> cached = TOP_COUNTRIES_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<CountryEventStat> results = eventRepository.queryTopCountriesByEvents(safeLimit);
        synchronized (TOP_COUNTRIES_CACHE) {
            TOP_COUNTRIES_CACHE.put(key, results);
        }
        return results;
    }

    public List<MonthlyTrendPoint> dailyTrend() {
        CacheVersion key = cacheVersion();
        synchronized (DAILY_TREND_CACHE) {
            List<MonthlyTrendPoint> cached = DAILY_TREND_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<MonthlyTrendPoint> results = eventRepository.queryOverallDailyTrend();
        synchronized (DAILY_TREND_CACHE) {
            DAILY_TREND_CACHE.put(key, results);
        }
        return results;
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

    private record SummaryCacheKey(CacheVersion version, int countryCount) {
    }

    private record TopCountriesCacheKey(CacheVersion version, int limit) {
    }
}
