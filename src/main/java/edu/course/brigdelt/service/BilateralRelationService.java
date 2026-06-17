package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.util.List;
import java.util.Locale;

/**
 * Provides validated bilateral relation analysis queries.
 */
public class BilateralRelationService {

    public static final String DEFAULT_COUNTRY_A = "CHN";
    public static final int DEFAULT_EVENT_LIMIT = 200;

    private final GdeltEventRepository eventRepository;

    public BilateralRelationService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public BilateralRelationService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
    }

    public BilateralRelationSummary summarize(String countryA, String countryB) {
        CountryPair pair = normalizePair(countryA, countryB);
        return eventRepository.summarizeBilateral(pair.countryA(), pair.countryB());
    }

    public List<EventQueryResult> events(String countryA, String countryB, int limit) {
        CountryPair pair = normalizePair(countryA, countryB);
        int safeLimit = limit <= 0 ? DEFAULT_EVENT_LIMIT : Math.min(limit, 1_000);
        return eventRepository.queryBilateralEvents(pair.countryA(), pair.countryB(), safeLimit);
    }

    public List<MonthlyTrendPoint> monthlyTrend(String countryA, String countryB) {
        CountryPair pair = normalizePair(countryA, countryB);
        return eventRepository.queryBilateralMonthlyTrend(pair.countryA(), pair.countryB());
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

    private record CountryPair(String countryA, String countryB) {
    }
}
