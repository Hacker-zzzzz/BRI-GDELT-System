package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.CountryEventStat;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventSubtypeStat;
import edu.course.brigdelt.domain.GeoEventPoint;
import edu.course.brigdelt.domain.GdeltEvent;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.domain.RegionSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists cleaned GDELT events into SQLite.
 */
public class GdeltEventRepository {

    private final DatabaseManager databaseManager;

    public GdeltEventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int insertIgnoreBatch(List<GdeltEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT OR IGNORE INTO gdelt_events (
                    global_event_id,
                    event_date,
                    actor1_country_code,
                    actor2_country_code,
                    event_code,
                    event_base_code,
                    event_root_code,
                    event_type,
                    goldstein_scale,
                    num_mentions,
                    avg_tone,
                    action_geo_lat,
                    action_geo_lon,
                    source_file
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                for (GdeltEvent event : events) {
                    statement.setString(1, toText(event.globalEventId()));
                    statement.setString(2, toText(event.eventDate()));
                    statement.setString(3, event.actor1CountryCode());
                    statement.setString(4, event.actor2CountryCode());
                    statement.setString(5, event.eventCode());
                    statement.setString(6, event.eventBaseCode());
                    statement.setString(7, event.eventRootCode());
                    statement.setString(8, toEventTypeText(event.eventType()));
                    setNullableDouble(statement, 9, event.goldsteinScale());
                    setNullableInt(statement, 10, event.numMentions());
                    setNullableDouble(statement, 11, event.avgTone());
                    setNullableDouble(statement, 12, event.actionGeoLat());
                    setNullableDouble(statement, 13, event.actionGeoLon());
                    statement.setString(14, event.sourceFile());
                    statement.addBatch();
                }

                int insertedRows = countInsertedRows(statement.executeBatch());
                connection.commit();
                return insertedRows;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("GDELT 事件批量入库失败。", exception);
        }
    }

    public int countEvents() {
        return countBySql("SELECT COUNT(*) FROM gdelt_events");
    }

    public int countEventsByType(EventType type) {
        String sql = "SELECT COUNT(*) FROM gdelt_events WHERE event_type = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, toEventTypeText(type));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("按事件类型统计 GDELT 事件数量失败。", exception);
        }
    }

    public List<EventQueryResult> queryEvents(EventQueryCriteria criteria) {
        QueryParts queryParts = buildWhereClause(criteria);
        String sql = """
                SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                       event_type, event_root_code, goldstein_scale, num_mentions, avg_tone, source_file
                FROM gdelt_events
                %s
                ORDER BY event_date DESC, global_event_id DESC
                LIMIT ?
                """.formatted(queryParts.whereClause());
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, queryParts.parameters());
            statement.setInt(queryParts.parameters().size() + 1, effectiveLimit(criteria));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EventQueryResult> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapQueryResult(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("查询 GDELT 事件失败。", exception);
        }
    }

    public int countEvents(EventQueryCriteria criteria) {
        QueryParts queryParts = buildWhereClause(criteria);
        String sql = "SELECT COUNT(*) FROM gdelt_events " + queryParts.whereClause();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, queryParts.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("统计 GDELT 查询结果失败。", exception);
        }
    }

    public List<EventSubtypeStat> queryEventSubtypeStats(EventQueryCriteria criteria, EventType eventType) {
        QueryParts queryParts = buildWhereClause(criteria);
        List<Object> parameters = new ArrayList<>(queryParts.parameters());
        String whereClause = queryParts.whereClause();
        String typeClause = "event_type = ?";
        if (whereClause.isBlank()) {
            whereClause = "WHERE " + typeClause;
        } else {
            whereClause = whereClause + " AND " + typeClause;
        }
        parameters.add(toEventTypeText(eventType));

        String sql = """
                SELECT event_root_code, COUNT(*) AS event_count
                FROM gdelt_events
                %s
                GROUP BY event_root_code
                ORDER BY event_root_code
                """.formatted(whereClause);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EventSubtypeStat> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new EventSubtypeStat(
                            resultSet.getString("event_root_code"),
                            "",
                            resultSet.getInt("event_count")
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("事件子类分布查询失败。", exception);
        }
    }

    public BilateralRelationSummary summarizeBilateral(String countryA, String countryB) {
        String sql = """
                SELECT
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    SUM(CASE WHEN event_type = 'OTHER' THEN 1 ELSE 0 END) AS other_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
                FROM gdelt_events
                WHERE (actor1_country_code = ? AND actor2_country_code = ?)
                   OR (actor1_country_code = ? AND actor2_country_code = ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCountryPair(statement, countryA, countryB);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return BilateralRelationSummary.empty(countryA, countryB);
                }
                int totalEvents = resultSet.getInt("total_events");
                int cooperationEvents = resultSet.getInt("cooperation_events");
                int conflictEvents = resultSet.getInt("conflict_events");
                int otherEvents = resultSet.getInt("other_events");
                return new BilateralRelationSummary(
                        countryA,
                        countryB,
                        totalEvents,
                        cooperationEvents,
                        conflictEvents,
                        otherEvents,
                        ratio(cooperationEvents, totalEvents),
                        ratio(conflictEvents, totalEvents),
                        resultSet.getDouble("average_goldstein"),
                        resultSet.getDouble("average_avg_tone"),
                        resultSet.getInt("total_mentions")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("双边关系统计失败。", exception);
        }
    }

    public List<EventQueryResult> queryBilateralEvents(String countryA, String countryB, int limit) {
        String sql = """
                SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                       event_type, event_root_code, goldstein_scale, num_mentions, avg_tone, source_file
                FROM gdelt_events
                WHERE (actor1_country_code = ? AND actor2_country_code = ?)
                   OR (actor1_country_code = ? AND actor2_country_code = ?)
                ORDER BY event_date DESC, global_event_id DESC
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCountryPair(statement, countryA, countryB);
            statement.setInt(5, limit <= 0 ? 200 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EventQueryResult> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapQueryResult(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("双边事件查询失败。", exception);
        }
    }

    public List<MonthlyTrendPoint> queryBilateralMonthlyTrend(String countryA, String countryB) {
        String sql = """
                SELECT
                    substr(event_date, 1, 7) AS month,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone
                FROM gdelt_events
                WHERE (actor1_country_code = ? AND actor2_country_code = ?)
                   OR (actor1_country_code = ? AND actor2_country_code = ?)
                GROUP BY substr(event_date, 1, 7)
                ORDER BY month
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCountryPair(statement, countryA, countryB);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MonthlyTrendPoint> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new MonthlyTrendPoint(
                            resultSet.getString("month"),
                            resultSet.getInt("total_events"),
                            resultSet.getInt("cooperation_events"),
                            resultSet.getInt("conflict_events"),
                            resultSet.getDouble("average_goldstein"),
                            resultSet.getDouble("average_avg_tone")
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("双边月度趋势查询失败。", exception);
        }
    }

    public DashboardSummary loadDashboardSummary(int countryCount, int importBatches) {
        String sql = """
                SELECT
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    SUM(CASE WHEN event_type = 'OTHER' THEN 1 ELSE 0 END) AS other_events,
                    SUM(num_mentions) AS total_mentions,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone
                FROM gdelt_events
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new DashboardSummary(countryCount, 0, 0, 0, 0, importBatches, 0, 0, 0);
            }
            return new DashboardSummary(
                    countryCount,
                    resultSet.getInt("total_events"),
                    resultSet.getInt("cooperation_events"),
                    resultSet.getInt("conflict_events"),
                    resultSet.getInt("other_events"),
                    importBatches,
                    resultSet.getInt("total_mentions"),
                    resultSet.getDouble("average_goldstein"),
                    resultSet.getDouble("average_avg_tone")
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("首页仪表盘统计失败。", exception);
        }
    }

    public List<CountryEventStat> queryTopCountriesByEvents(int limit) {
        String sql = """
                SELECT c.cameo_code AS country_code, COUNT(*) AS event_count
                FROM (
                    SELECT actor1_country_code AS country_code FROM gdelt_events WHERE actor1_country_code IS NOT NULL
                    UNION ALL
                    SELECT actor2_country_code AS country_code FROM gdelt_events WHERE actor2_country_code IS NOT NULL
                ) ce
                JOIN countries c ON c.cameo_code = ce.country_code AND c.is_bri_country = 1
                GROUP BY c.cameo_code
                ORDER BY event_count DESC, country_code
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 8 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountryEventStat> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new CountryEventStat(
                            resultSet.getString("country_code"),
                            resultSet.getInt("event_count")
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("国家事件量排行查询失败。", exception);
        }
    }

    public List<MonthlyTrendPoint> queryOverallMonthlyTrend() {
        String sql = """
                SELECT
                    substr(event_date, 1, 7) AS month,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone
                FROM gdelt_events
                GROUP BY substr(event_date, 1, 7)
                ORDER BY month
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<MonthlyTrendPoint> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(new MonthlyTrendPoint(
                        resultSet.getString("month"),
                        resultSet.getInt("total_events"),
                        resultSet.getInt("cooperation_events"),
                        resultSet.getInt("conflict_events"),
                        resultSet.getDouble("average_goldstein"),
                        resultSet.getDouble("average_avg_tone")
                ));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("整体月度趋势查询失败。", exception);
        }
    }

    public List<MonthlyTrendPoint> queryOverallDailyTrend() {
        String sql = """
                SELECT
                    substr(source_file, 1, 4) || '-' || substr(source_file, 5, 2) || '-' || substr(source_file, 7, 2) AS day,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone
                FROM gdelt_events
                WHERE source_file IS NOT NULL
                  AND length(source_file) >= 8
                  AND substr(source_file, 1, 8) GLOB '[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'
                GROUP BY substr(source_file, 1, 8)
                ORDER BY day
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<MonthlyTrendPoint> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(new MonthlyTrendPoint(
                        resultSet.getString("day"),
                        resultSet.getInt("total_events"),
                        resultSet.getInt("cooperation_events"),
                        resultSet.getInt("conflict_events"),
                        resultSet.getDouble("average_goldstein"),
                        resultSet.getDouble("average_avg_tone")
                ));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("整体日度趋势查询失败。", exception);
        }
    }

    public List<CooperationScore> queryCooperationScores(int limit) {
        String sql = """
                WITH country_events AS (
                    SELECT actor1_country_code AS country_code, event_type, goldstein_scale, avg_tone, num_mentions
                    FROM gdelt_events
                    WHERE actor1_country_code IS NOT NULL AND TRIM(actor1_country_code) <> ''
                    UNION ALL
                    SELECT actor2_country_code AS country_code, event_type, goldstein_scale, avg_tone, num_mentions
                    FROM gdelt_events
                    WHERE actor2_country_code IS NOT NULL AND TRIM(actor2_country_code) <> ''
                )
                SELECT
                    c.cameo_code AS country_code,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
                FROM country_events
                JOIN countries c ON c.cameo_code = country_events.country_code AND c.is_bri_country = 1
                GROUP BY c.cameo_code
                ORDER BY cooperation_events DESC, total_events DESC, country_code
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 20 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CooperationScore> results = new ArrayList<>();
                while (resultSet.next()) {
                    int cooperationEvents = resultSet.getInt("cooperation_events");
                    int conflictEvents = resultSet.getInt("conflict_events");
                    double averageGoldstein = resultSet.getDouble("average_goldstein");
                    double averageAvgTone = resultSet.getDouble("average_avg_tone");
                    int totalMentions = resultSet.getInt("total_mentions");
                    results.add(new CooperationScore(
                            resultSet.getString("country_code"),
                            resultSet.getInt("total_events"),
                            cooperationEvents,
                            conflictEvents,
                            averageGoldstein,
                            averageAvgTone,
                            totalMentions,
                            cooperationIndex(cooperationEvents, conflictEvents, averageGoldstein,
                                    averageAvgTone, totalMentions)
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("合作态势国家排名查询失败。", exception);
        }
    }

    public List<RiskAssessment> queryRiskAssessments(int limit) {
        String sql = """
                WITH country_events AS (
                    SELECT actor1_country_code AS country_code, event_type, goldstein_scale, avg_tone
                    FROM gdelt_events
                    WHERE actor1_country_code IS NOT NULL AND TRIM(actor1_country_code) <> ''
                    UNION ALL
                    SELECT actor2_country_code AS country_code, event_type, goldstein_scale, avg_tone
                    FROM gdelt_events
                    WHERE actor2_country_code IS NOT NULL AND TRIM(actor2_country_code) <> ''
                )
                SELECT
                    c.cameo_code AS country_code,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone
                FROM country_events
                JOIN countries c ON c.cameo_code = country_events.country_code AND c.is_bri_country = 1
                GROUP BY c.cameo_code
                ORDER BY conflict_events DESC, total_events DESC, country_code
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 20 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RiskAssessment> results = new ArrayList<>();
                while (resultSet.next()) {
                    int totalEvents = resultSet.getInt("total_events");
                    int conflictEvents = resultSet.getInt("conflict_events");
                    double conflictRatio = ratio(conflictEvents, totalEvents);
                    double averageGoldstein = resultSet.getDouble("average_goldstein");
                    double averageAvgTone = resultSet.getDouble("average_avg_tone");
                    double riskIndex = riskIndex(conflictEvents, conflictRatio, averageGoldstein, averageAvgTone);
                    results.add(new RiskAssessment(
                            resultSet.getString("country_code"),
                            totalEvents,
                            conflictEvents,
                            conflictRatio,
                            averageGoldstein,
                            averageAvgTone,
                            riskIndex,
                            riskLevel(riskIndex)
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("风险评估国家排名查询失败。", exception);
        }
    }

    public List<GeoEventPoint> queryGeoEventPoints(int limit) {
        String sql = """
                SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                       event_type, action_geo_lat, action_geo_lon, goldstein_scale, avg_tone
                FROM gdelt_events
                WHERE action_geo_lat IS NOT NULL
                  AND action_geo_lon IS NOT NULL
                  AND action_geo_lat BETWEEN -90 AND 90
                  AND action_geo_lon BETWEEN -180 AND 180
                ORDER BY event_date DESC, global_event_id DESC
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 500 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GeoEventPoint> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new GeoEventPoint(
                            resultSet.getString("global_event_id"),
                            LocalDate.parse(resultSet.getString("event_date")),
                            resultSet.getString("actor1_country_code"),
                            resultSet.getString("actor2_country_code"),
                            parseEventType(resultSet.getString("event_type")),
                            resultSet.getDouble("action_geo_lat"),
                            resultSet.getDouble("action_geo_lon"),
                            resultSet.getDouble("goldstein_scale"),
                            resultSet.getDouble("avg_tone")
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("专题地图事件点位查询失败。", exception);
        }
    }

    public List<RegionSummary> queryRegionSummaries() {
        String sql = """
                WITH region_country_events AS (
                    SELECT c.region, c.cameo_code, e.event_type, e.goldstein_scale, e.avg_tone, e.num_mentions
                    FROM countries c
                    JOIN gdelt_events e ON e.actor1_country_code = c.cameo_code
                    WHERE c.is_bri_country = 1
                    UNION ALL
                    SELECT c.region, c.cameo_code, e.event_type, e.goldstein_scale, e.avg_tone, e.num_mentions
                    FROM countries c
                    JOIN gdelt_events e ON e.actor2_country_code = c.cameo_code
                    WHERE c.is_bri_country = 1
                ),
                region_country_counts AS (
                    SELECT region, COUNT(*) AS country_count
                    FROM countries
                    WHERE is_bri_country = 1
                    GROUP BY region
                )
                SELECT
                    r.region,
                    r.country_count,
                    COUNT(e.cameo_code) AS total_events,
                    SUM(CASE WHEN e.event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN e.event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(e.goldstein_scale) AS average_goldstein,
                    AVG(e.avg_tone) AS average_avg_tone,
                    SUM(e.num_mentions) AS total_mentions
                FROM region_country_counts r
                LEFT JOIN region_country_events e ON e.region = r.region
                GROUP BY r.region, r.country_count
                ORDER BY r.region
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {
            List<RegionSummary> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(new RegionSummary(
                        resultSet.getString("region"),
                        resultSet.getInt("country_count"),
                        resultSet.getInt("total_events"),
                        resultSet.getInt("cooperation_events"),
                        resultSet.getInt("conflict_events"),
                        resultSet.getDouble("average_goldstein"),
                        resultSet.getDouble("average_avg_tone"),
                        resultSet.getInt("total_mentions"),
                        0,
                        0
                ));
            }
            return normalizeRegionIndexes(results);
        } catch (SQLException exception) {
            throw new IllegalStateException("区域汇总分析查询失败。", exception);
        }
    }

    private List<RegionSummary> normalizeRegionIndexes(List<RegionSummary> summaries) {
        int maxEvents = summaries.stream().mapToInt(RegionSummary::totalEvents).max().orElse(0);
        int maxConflicts = summaries.stream().mapToInt(RegionSummary::conflictEvents).max().orElse(0);
        int maxMentions = summaries.stream().mapToInt(RegionSummary::totalMentions).max().orElse(0);
        List<RegionSummary> normalized = new ArrayList<>();
        for (RegionSummary summary : summaries) {
            normalized.add(new RegionSummary(
                    summary.region(),
                    summary.countryCount(),
                    summary.totalEvents(),
                    summary.cooperationEvents(),
                    summary.conflictEvents(),
                    summary.averageGoldstein(),
                    summary.averageAvgTone(),
                    summary.totalMentions(),
                    regionCooperationIndex(summary, maxEvents, maxMentions),
                    regionRiskIndex(summary, maxConflicts, maxMentions)
            ));
        }
        return normalized;
    }

    private double regionCooperationIndex(RegionSummary summary, int maxEvents, int maxMentions) {
        if (summary.totalEvents() <= 0) {
            return 0;
        }
        double cooperationRatio = ratio(summary.cooperationEvents(), summary.totalEvents());
        double positiveGoldstein = Math.max(summary.averageGoldstein(), 0) / 10.0;
        double positiveTone = Math.max(summary.averageAvgTone(), 0) / 100.0;
        double mentionShare = maxMentions <= 0 ? 0 : (double) summary.totalMentions() / maxMentions;
        double eventShare = maxEvents <= 0 ? 0 : (double) summary.totalEvents() / maxEvents;
        return clamp(100.0 * (
                cooperationRatio * 0.45
                        + positiveGoldstein * 0.20
                        + positiveTone * 0.10
                        + mentionShare * 0.15
                        + eventShare * 0.10
        ));
    }

    private double regionRiskIndex(RegionSummary summary, int maxConflicts, int maxMentions) {
        if (summary.totalEvents() <= 0) {
            return 0;
        }
        double conflictRatio = ratio(summary.conflictEvents(), summary.totalEvents());
        double negativeGoldstein = Math.max(-summary.averageGoldstein(), 0) / 10.0;
        double negativeTone = Math.max(-summary.averageAvgTone(), 0) / 100.0;
        double mentionShare = maxMentions <= 0 ? 0 : (double) summary.totalMentions() / maxMentions;
        double conflictShare = maxConflicts <= 0 ? 0 : (double) summary.conflictEvents() / maxConflicts;
        return clamp(100.0 * (
                conflictRatio * 0.45
                        + negativeGoldstein * 0.20
                        + negativeTone * 0.10
                        + mentionShare * 0.10
                        + conflictShare * 0.15
        ));
    }

    private QueryParts buildWhereClause(EventQueryCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        if (criteria != null) {
            if (criteria.startDate() != null) {
                clauses.add("event_date >= ?");
                parameters.add(criteria.startDate().toString());
            }
            if (criteria.endDate() != null) {
                clauses.add("event_date <= ?");
                parameters.add(criteria.endDate().toString());
            }
            if (hasText(criteria.anyCountryCode())) {
                clauses.add("(actor1_country_code = ? OR actor2_country_code = ?)");
                parameters.add(criteria.anyCountryCode());
                parameters.add(criteria.anyCountryCode());
            }
            if (hasText(criteria.actor1CountryCode())) {
                clauses.add("actor1_country_code = ?");
                parameters.add(criteria.actor1CountryCode());
            }
            if (hasText(criteria.actor2CountryCode())) {
                clauses.add("actor2_country_code = ?");
                parameters.add(criteria.actor2CountryCode());
            }
            if (hasText(criteria.region())) {
                clauses.add("""
                        EXISTS (
                            SELECT 1
                            FROM countries c
                            WHERE c.is_bri_country = 1
                              AND c.region = ?
                              AND (c.cameo_code = actor1_country_code OR c.cameo_code = actor2_country_code)
                        )
                        """);
                parameters.add(criteria.region());
            }
            if (criteria.eventType() != null) {
                clauses.add("event_type = ?");
                parameters.add(criteria.eventType().name());
            }
        }
        String whereClause = clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", clauses);
        return new QueryParts(whereClause, parameters);
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private void bindCountryPair(PreparedStatement statement, String countryA, String countryB) throws SQLException {
        statement.setString(1, countryA);
        statement.setString(2, countryB);
        statement.setString(3, countryB);
        statement.setString(4, countryA);
    }

    private double ratio(int value, int total) {
        return total <= 0 ? 0 : (double) value / total;
    }

    private double cooperationIndex(int cooperationEvents, int conflictEvents, double averageGoldstein,
                                    double averageAvgTone, int totalMentions) {
        return clamp(cooperationEvents * 2.0
                + Math.max(averageGoldstein, 0) * 6.0
                + Math.max(averageAvgTone, 0) * 2.0
                + Math.log1p(Math.max(totalMentions, 0)) * 4.0
                - conflictEvents * 1.5);
    }

    private double riskIndex(int conflictEvents, double conflictRatio, double averageGoldstein, double averageAvgTone) {
        return clamp(conflictRatio * 60.0
                + Math.max(-averageGoldstein, 0) * 6.0
                + Math.max(-averageAvgTone, 0) * 2.5
                + conflictEvents * 2.0);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private String riskLevel(double riskIndex) {
        if (riskIndex < 25) {
            return "低";
        }
        if (riskIndex < 50) {
            return "中";
        }
        if (riskIndex < 75) {
            return "高";
        }
        return "极高";
    }

    private EventQueryResult mapQueryResult(ResultSet resultSet) throws SQLException {
        return new EventQueryResult(
                resultSet.getString("global_event_id"),
                LocalDate.parse(resultSet.getString("event_date")),
                resultSet.getString("actor1_country_code"),
                resultSet.getString("actor2_country_code"),
                parseEventType(resultSet.getString("event_type")),
                resultSet.getString("event_root_code"),
                resultSet.getDouble("goldstein_scale"),
                resultSet.getInt("num_mentions"),
                resultSet.getDouble("avg_tone"),
                resultSet.getString("source_file")
        );
    }

    private EventType parseEventType(String value) {
        if (!hasText(value)) {
            return EventType.OTHER;
        }
        try {
            return EventType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return EventType.OTHER;
        }
    }

    private int effectiveLimit(EventQueryCriteria criteria) {
        if (criteria == null || criteria.limit() <= 0) {
            return EventQueryCriteria.DEFAULT_LIMIT;
        }
        return Math.min(criteria.limit(), EventQueryCriteria.MAX_LIMIT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int countBySql(String sql) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("统计 GDELT 事件数量失败。", exception);
        }
    }

    private int countInsertedRows(int[] updateCounts) {
        int insertedRows = 0;
        for (int updateCount : updateCounts) {
            if (updateCount > 0) {
                insertedRows += updateCount;
            } else if (updateCount == Statement.SUCCESS_NO_INFO) {
                throw new IllegalStateException("数据库未返回实际插入行数，无法确认批量入库结果。");
            }
        }
        return insertedRows;
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String toEventTypeText(EventType type) {
        return type == null ? null : type.name();
    }

    private void setNullableDouble(PreparedStatement statement, int parameterIndex, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.REAL);
            return;
        }
        statement.setDouble(parameterIndex, value);
    }

    private void setNullableInt(PreparedStatement statement, int parameterIndex, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.INTEGER);
            return;
        }
        statement.setInt(parameterIndex, value);
    }

    private void rollback(Connection connection, SQLException originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private record QueryParts(String whereClause, List<Object> parameters) {
    }
}
