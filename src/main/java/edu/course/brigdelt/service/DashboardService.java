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
 * 首页仪表盘服务。
 *
 * <p>集中加载总览指标、国家热度和日度趋势，并通过数据版本缓存减少重复查询。</p>
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

    /**
     * 加载首页总览指标，包括国家数量、事件总量、事件结构和导入批次。
     */
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

    /**
     * 查询事件量最高的国家，用于首页柱状图展示热点国家。
     */
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

    /**
     * 查询日度事件趋势，用于首页展示数据时间分布。
     */
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
        // 数据版本随事件数量和导入批次数变化，保证导入后缓存能自动刷新。
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
