package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.CountryEventStat;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;

import java.util.List;

/**
 * Loads aggregated values used by the dashboard page.
 */
public class DashboardService {

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
        return eventRepository.loadDashboardSummary(
                countryRepository.countCountries(),
                importBatchRepository.countImportBatches()
        );
    }

    public List<CountryEventStat> topCountries(int limit) {
        return eventRepository.queryTopCountriesByEvents(limit <= 0 ? 8 : limit);
    }

    public List<MonthlyTrendPoint> dailyTrend() {
        return eventRepository.queryOverallDailyTrend();
    }
}
