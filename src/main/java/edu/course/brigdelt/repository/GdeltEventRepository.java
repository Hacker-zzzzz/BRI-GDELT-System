package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.CooperationHotspot;
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
import edu.course.brigdelt.domain.RiskHotspot;
import edu.course.brigdelt.domain.RegionSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * GDELT 事件数据仓储类，负责清洗后事件的 SQLite 持久化和统计查询。
 *
 * <p>仪表盘、双边关系、合作排名、风险评估、区域汇总和地图页面都依赖这里的
 * SQL 聚合结果。将聚合计算尽量放在数据库侧完成，可以避免把完整 GDELT 事件表
 * 一次性加载到内存中，适合课程演示数据量较大的场景。</p>
 */
public class GdeltEventRepository {

    private final DatabaseManager databaseManager;

    public GdeltEventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 批量写入清洗后的 GDELT 事件，使用 INSERT OR IGNORE 避免重复事件编号导致导入中断。
     */
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

    /**
     * 按多条件查询事件明细，结果用于“事件查询”页面的表格展示。
     */
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

    /**
     * 统计同一查询条件下的事件总数，用于界面展示命中规模。
     */
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

    /**
     * 统计某类事件根代码分布，用于合作/冲突子类型饼图。
     */
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

    /**
     * 汇总两个国家之间的双边关系，兼容 Actor1/Actor2 顺序互换的事件记录。
     */
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

    /**
     * 查询双边关系明细事件，用于双边页面下方表格追溯原始事件。
     */
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

    /**
     * 按月份汇总双边趋势，支撑双边关系折线图和趋势表。
     */
    public List<MonthlyTrendPoint> queryBilateralMonthlyTrend(String countryA, String countryB) {
        String sql = """
                SELECT
                    substr(event_date, 1, 7) AS month,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
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
                    int cooperationEvents = resultSet.getInt("cooperation_events");
                    int conflictEvents = resultSet.getInt("conflict_events");
                    double averageGoldstein = resultSet.getDouble("average_goldstein");
                    double averageAvgTone = resultSet.getDouble("average_avg_tone");
                    int totalMentions = resultSet.getInt("total_mentions");
                    results.add(new MonthlyTrendPoint(
                            resultSet.getString("month"),
                            resultSet.getInt("total_events"),
                            cooperationEvents,
                            conflictEvents,
                            averageGoldstein,
                            averageAvgTone,
                            cooperationIndex(cooperationEvents, conflictEvents, averageGoldstein,
                                    averageAvgTone, totalMentions)
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("双边月度趋势查询失败。", exception);
        }
    }

    /**
     * 加载首页仪表盘总览指标，包括事件结构、媒体关注度和平均语调。
     */
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

    /**
     * 查询事件量最高的国家，用于首页国家热度柱状图。
     */
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

    /**
     * 汇总全局月度趋势，用于观察数据整体时间变化。
     */
    public List<MonthlyTrendPoint> queryOverallMonthlyTrend() {
        String sql = """
                SELECT
                    substr(event_date, 1, 7) AS month,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
                FROM gdelt_events
                GROUP BY substr(event_date, 1, 7)
                ORDER BY month
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<MonthlyTrendPoint> results = new ArrayList<>();
            while (resultSet.next()) {
                int cooperationEvents = resultSet.getInt("cooperation_events");
                int conflictEvents = resultSet.getInt("conflict_events");
                double averageGoldstein = resultSet.getDouble("average_goldstein");
                double averageAvgTone = resultSet.getDouble("average_avg_tone");
                int totalMentions = resultSet.getInt("total_mentions");
                results.add(new MonthlyTrendPoint(
                        resultSet.getString("month"),
                        resultSet.getInt("total_events"),
                        cooperationEvents,
                        conflictEvents,
                        averageGoldstein,
                        averageAvgTone,
                        cooperationIndex(cooperationEvents, conflictEvents, averageGoldstein,
                                averageAvgTone, totalMentions)
                ));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("整体月度趋势查询失败。", exception);
        }
    }

    /**
     * 汇总全局日度趋势，用于首页更细粒度的导入数据变化展示。
     */
    public List<MonthlyTrendPoint> queryOverallDailyTrend() {
        String sql = """
                SELECT
                    substr(source_file, 1, 4) || '-' || substr(source_file, 5, 2) || '-' || substr(source_file, 7, 2) AS day,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
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
                int cooperationEvents = resultSet.getInt("cooperation_events");
                int conflictEvents = resultSet.getInt("conflict_events");
                double averageGoldstein = resultSet.getDouble("average_goldstein");
                double averageAvgTone = resultSet.getDouble("average_avg_tone");
                int totalMentions = resultSet.getInt("total_mentions");
                results.add(new MonthlyTrendPoint(
                        resultSet.getString("day"),
                        resultSet.getInt("total_events"),
                        cooperationEvents,
                        conflictEvents,
                        averageGoldstein,
                        averageAvgTone,
                        cooperationIndex(cooperationEvents, conflictEvents, averageGoldstein,
                                averageAvgTone, totalMentions)
                ));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("整体日度趋势查询失败。", exception);
        }
    }

    /**
     * 查询国家合作指数排名，作为合作态势分析页和导出 CSV 的核心数据。
     */
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
                    c.name_cn AS country_name,
                    c.region AS region,
                    COUNT(*) AS total_events,
                    SUM(CASE WHEN event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                    SUM(CASE WHEN event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                    AVG(goldstein_scale) AS average_goldstein,
                    AVG(avg_tone) AS average_avg_tone,
                    SUM(num_mentions) AS total_mentions
                FROM country_events
                JOIN countries c ON c.cameo_code = country_events.country_code AND c.is_bri_country = 1
                GROUP BY c.cameo_code, c.name_cn, c.region
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountryCooperationAggregate> aggregates = new ArrayList<>();
                while (resultSet.next()) {
                    aggregates.add(new CountryCooperationAggregate(
                            resultSet.getString("country_code"),
                            resultSet.getString("country_name"),
                            resultSet.getString("region"),
                            resultSet.getInt("total_events"),
                            resultSet.getInt("cooperation_events"),
                            resultSet.getInt("conflict_events"),
                            resultSet.getDouble("average_goldstein"),
                            resultSet.getDouble("average_avg_tone"),
                            resultSet.getInt("total_mentions")
                    ));
                }
                int maxCooperationEvents = aggregates.stream()
                        .mapToInt(CountryCooperationAggregate::cooperationEvents)
                        .max()
                        .orElse(1);
                int maxMentions = aggregates.stream()
                        .mapToInt(CountryCooperationAggregate::totalMentions)
                        .max()
                        .orElse(1);
                int safeLimit = limit <= 0 ? 50 : limit;
                return aggregates.stream()
                        .map(aggregate -> new CooperationScore(
                                aggregate.countryCode(),
                                aggregate.countryName(),
                                normalizeRegion(aggregate.region()),
                                aggregate.totalEvents(),
                                aggregate.cooperationEvents(),
                                aggregate.conflictEvents(),
                                aggregate.averageGoldstein(),
                                aggregate.averageAvgTone(),
                                aggregate.totalMentions(),
                                cooperationIndex(aggregate, maxCooperationEvents, maxMentions)
                        ))
                        .sorted(Comparator.comparingDouble(CooperationScore::cooperationIndex).reversed()
                                .thenComparing(Comparator.comparingInt(CooperationScore::cooperationEvents).reversed())
                                .thenComparing(CooperationScore::countryCode))
                        .limit(safeLimit)
                        .toList();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("合作态势国家排名查询失败。", exception);
        }
    }

    /**
     * 查询合作热点，比较最近两个月合作指数和合作事件增量的变化。
     */
    public List<CooperationHotspot> queryCooperationHotspots(int limit) {
        String sql = """
                WITH available_months AS (
                    SELECT substr(event_date, 1, 7) AS month
                    FROM gdelt_events
                    WHERE event_date IS NOT NULL AND length(event_date) >= 7
                    GROUP BY substr(event_date, 1, 7)
                ),
                adjacent_pair AS (
                    SELECT previous.month AS previous_month, current.month AS current_month
                    FROM available_months current
                    JOIN available_months previous
                      ON previous.month = strftime('%Y-%m', date(current.month || '-01', '-1 month'))
                    ORDER BY current.month DESC
                    LIMIT 1
                ),
                recent_months AS (
                    SELECT previous_month AS month, 1 AS month_index
                    FROM adjacent_pair
                    UNION ALL
                    SELECT current_month AS month, 2 AS month_index
                    FROM adjacent_pair
                ),
                month_order AS (
                    SELECT month, month_index
                    FROM recent_months
                ),
                latest_month AS (
                    SELECT month
                    FROM month_order
                    WHERE month_index = 2
                ),
                day_cutoff AS (
                    SELECT MAX(CAST(substr(event_date, 9, 2) AS INTEGER)) AS max_day
                    FROM gdelt_events
                    WHERE substr(event_date, 1, 7) = (SELECT month FROM latest_month)
                ),
                country_month_events AS (
                    SELECT actor1_country_code AS country_code, substr(event_date, 1, 7) AS month,
                           event_type, goldstein_scale, avg_tone, num_mentions
                    FROM gdelt_events
                    WHERE actor1_country_code IS NOT NULL AND TRIM(actor1_country_code) <> ''
                      AND substr(event_date, 1, 7) IN (SELECT month FROM recent_months)
                      AND CAST(substr(event_date, 9, 2) AS INTEGER) <= (SELECT max_day FROM day_cutoff)
                    UNION ALL
                    SELECT actor2_country_code AS country_code, substr(event_date, 1, 7) AS month,
                           event_type, goldstein_scale, avg_tone, num_mentions
                    FROM gdelt_events
                    WHERE actor2_country_code IS NOT NULL AND TRIM(actor2_country_code) <> ''
                      AND substr(event_date, 1, 7) IN (SELECT month FROM recent_months)
                      AND CAST(substr(event_date, 9, 2) AS INTEGER) <= (SELECT max_day FROM day_cutoff)
                ),
                country_month_scores AS (
                    SELECT c.cameo_code AS country_code,
                           c.name_cn AS country_name,
                           c.region,
                           e.month,
                           SUM(CASE WHEN e.event_type = 'COOPERATION' THEN 1 ELSE 0 END) AS cooperation_events,
                           SUM(CASE WHEN e.event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                           AVG(e.goldstein_scale) AS average_goldstein,
                           AVG(e.avg_tone) AS average_avg_tone,
                           SUM(e.num_mentions) AS total_mentions
                    FROM country_month_events e
                    JOIN countries c ON c.cameo_code = e.country_code AND c.is_bri_country = 1
                    GROUP BY c.cameo_code, c.name_cn, c.region, e.month
                )
                SELECT current.country_code,
                       current.country_name,
                       current.region,
                       previous.month AS previous_month,
                       current.month AS current_month,
                       previous.cooperation_events AS previous_cooperation_events,
                       current.cooperation_events AS current_cooperation_events,
                       previous.conflict_events AS previous_conflict_events,
                       current.conflict_events AS current_conflict_events,
                       previous.average_goldstein AS previous_average_goldstein,
                       current.average_goldstein AS current_average_goldstein,
                       previous.average_avg_tone AS previous_average_avg_tone,
                       current.average_avg_tone AS current_average_avg_tone,
                       previous.total_mentions AS previous_total_mentions,
                       current.total_mentions AS current_total_mentions
                FROM country_month_scores current
                JOIN month_order current_month ON current_month.month = current.month AND current_month.month_index = 2
                JOIN country_month_scores previous ON previous.country_code = current.country_code
                JOIN month_order previous_month ON previous_month.month = previous.month AND previous_month.month_index = 1
                ORDER BY current.country_code
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 500 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CooperationHotspot> results = new ArrayList<>();
                while (resultSet.next()) {
                    double previousIndex = cooperationIndex(
                            resultSet.getInt("previous_cooperation_events"),
                            resultSet.getInt("previous_conflict_events"),
                            resultSet.getDouble("previous_average_goldstein"),
                            resultSet.getDouble("previous_average_avg_tone"),
                            resultSet.getInt("previous_total_mentions")
                    );
                    double currentIndex = cooperationIndex(
                            resultSet.getInt("current_cooperation_events"),
                            resultSet.getInt("current_conflict_events"),
                            resultSet.getDouble("current_average_goldstein"),
                            resultSet.getDouble("current_average_avg_tone"),
                            resultSet.getInt("current_total_mentions")
                    );
                    results.add(new CooperationHotspot(
                            resultSet.getString("country_code"),
                            resultSet.getString("country_name"),
                            resultSet.getString("region"),
                            resultSet.getString("previous_month"),
                            resultSet.getString("current_month"),
                            previousIndex,
                            currentIndex,
                            currentIndex - previousIndex,
                            resultSet.getInt("current_cooperation_events")
                                    - resultSet.getInt("previous_cooperation_events")
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("合作热点追踪查询失败。", exception);
        }
    }

    /**
     * 查询国家风险评估排名，综合冲突占比、冲突事件量和负向情绪信号。
     */
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
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CountryRiskAggregate> aggregates = new ArrayList<>();
                while (resultSet.next()) {
                    int totalEvents = resultSet.getInt("total_events");
                    int conflictEvents = resultSet.getInt("conflict_events");
                    aggregates.add(new CountryRiskAggregate(
                            resultSet.getString("country_code"),
                            totalEvents,
                            conflictEvents,
                            resultSet.getDouble("average_goldstein"),
                            resultSet.getDouble("average_avg_tone")
                    ));
                }
                int maxConflictEvents = aggregates.stream()
                        .mapToInt(CountryRiskAggregate::conflictEvents)
                        .max()
                        .orElse(1);
                int safeLimit = limit <= 0 ? 50 : limit;
                return aggregates.stream()
                        .map(aggregate -> {
                            double conflictRatio = ratio(aggregate.conflictEvents(), aggregate.totalEvents());
                            double riskIndex = riskIndex(aggregate, conflictRatio, maxConflictEvents);
                            return new RiskAssessment(
                                    aggregate.countryCode(),
                                    aggregate.totalEvents(),
                                    aggregate.conflictEvents(),
                                    conflictRatio,
                                    aggregate.averageGoldstein(),
                                    aggregate.averageAvgTone(),
                                    riskIndex,
                                    riskLevel(riskIndex)
                            );
                        })
                        .sorted(Comparator.comparingDouble(RiskAssessment::riskIndex).reversed()
                                .thenComparing(Comparator.comparingInt(RiskAssessment::conflictEvents).reversed())
                                .thenComparing(RiskAssessment::countryCode))
                        .limit(safeLimit)
                        .toList();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("风险评估国家排名查询失败。", exception);
        }
    }

    /**
     * 查询风险热点，识别最近月份冲突风险上升较明显的国家。
     */
    public List<RiskHotspot> queryRiskHotspots(int limit) {
        String sql = """
                WITH available_months AS (
                    SELECT substr(event_date, 1, 7) AS month
                    FROM gdelt_events
                    WHERE event_date IS NOT NULL AND length(event_date) >= 7
                    GROUP BY substr(event_date, 1, 7)
                ),
                adjacent_pair AS (
                    SELECT previous.month AS previous_month, current.month AS current_month
                    FROM available_months current
                    JOIN available_months previous
                      ON previous.month = strftime('%Y-%m', date(current.month || '-01', '-1 month'))
                    ORDER BY current.month DESC
                    LIMIT 1
                ),
                recent_months AS (
                    SELECT previous_month AS month, 1 AS month_index
                    FROM adjacent_pair
                    UNION ALL
                    SELECT current_month AS month, 2 AS month_index
                    FROM adjacent_pair
                ),
                month_order AS (
                    SELECT month, month_index
                    FROM recent_months
                ),
                latest_month AS (
                    SELECT month
                    FROM month_order
                    WHERE month_index = 2
                ),
                day_cutoff AS (
                    SELECT MAX(CAST(substr(event_date, 9, 2) AS INTEGER)) AS max_day
                    FROM gdelt_events
                    WHERE substr(event_date, 1, 7) = (SELECT month FROM latest_month)
                ),
                country_month_events AS (
                    SELECT actor1_country_code AS country_code, substr(event_date, 1, 7) AS month,
                           event_type, goldstein_scale, avg_tone
                    FROM gdelt_events
                    WHERE actor1_country_code IS NOT NULL AND TRIM(actor1_country_code) <> ''
                      AND substr(event_date, 1, 7) IN (SELECT month FROM recent_months)
                      AND CAST(substr(event_date, 9, 2) AS INTEGER) <= (SELECT max_day FROM day_cutoff)
                    UNION ALL
                    SELECT actor2_country_code AS country_code, substr(event_date, 1, 7) AS month,
                           event_type, goldstein_scale, avg_tone
                    FROM gdelt_events
                    WHERE actor2_country_code IS NOT NULL AND TRIM(actor2_country_code) <> ''
                      AND substr(event_date, 1, 7) IN (SELECT month FROM recent_months)
                      AND CAST(substr(event_date, 9, 2) AS INTEGER) <= (SELECT max_day FROM day_cutoff)
                ),
                country_month_scores AS (
                    SELECT c.cameo_code AS country_code,
                           c.name_cn AS country_name,
                           c.region,
                           e.month,
                           COUNT(*) AS total_events,
                           SUM(CASE WHEN e.event_type = 'CONFLICT' THEN 1 ELSE 0 END) AS conflict_events,
                           AVG(e.goldstein_scale) AS average_goldstein,
                           AVG(e.avg_tone) AS average_avg_tone
                    FROM country_month_events e
                    JOIN countries c ON c.cameo_code = e.country_code AND c.is_bri_country = 1
                    GROUP BY c.cameo_code, c.name_cn, c.region, e.month
                )
                SELECT current.country_code,
                       current.country_name,
                       current.region,
                       previous.month AS previous_month,
                       current.month AS current_month,
                       previous.total_events AS previous_total_events,
                       current.total_events AS current_total_events,
                       previous.conflict_events AS previous_conflict_events,
                       current.conflict_events AS current_conflict_events,
                       previous.average_goldstein AS previous_average_goldstein,
                       current.average_goldstein AS current_average_goldstein,
                       previous.average_avg_tone AS previous_average_avg_tone,
                       current.average_avg_tone AS current_average_avg_tone
                FROM country_month_scores current
                JOIN month_order current_month ON current_month.month = current.month AND current_month.month_index = 2
                JOIN country_month_scores previous ON previous.country_code = current.country_code
                JOIN month_order previous_month ON previous_month.month = previous.month AND previous_month.month_index = 1
                ORDER BY current.country_code
                LIMIT ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit <= 0 ? 500 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RiskHotspot> results = new ArrayList<>();
                while (resultSet.next()) {
                    int previousTotalEvents = resultSet.getInt("previous_total_events");
                    int currentTotalEvents = resultSet.getInt("current_total_events");
                    int previousConflictEvents = resultSet.getInt("previous_conflict_events");
                    int currentConflictEvents = resultSet.getInt("current_conflict_events");
                    double previousIndex = riskIndex(
                            previousConflictEvents,
                            ratio(previousConflictEvents, previousTotalEvents),
                            resultSet.getDouble("previous_average_goldstein"),
                            resultSet.getDouble("previous_average_avg_tone")
                    );
                    double currentIndex = riskIndex(
                            currentConflictEvents,
                            ratio(currentConflictEvents, currentTotalEvents),
                            resultSet.getDouble("current_average_goldstein"),
                            resultSet.getDouble("current_average_avg_tone")
                    );
                    results.add(new RiskHotspot(
                            resultSet.getString("country_code"),
                            resultSet.getString("country_name"),
                            resultSet.getString("region"),
                            resultSet.getString("previous_month"),
                            resultSet.getString("current_month"),
                            previousIndex,
                            currentIndex,
                            currentIndex - previousIndex,
                            currentConflictEvents - previousConflictEvents
                    ));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("风险热点追踪查询失败。", exception);
        }
    }

    /**
     * 查询最近的地理事件点，用于普通专题地图散点展示。
     */
    public List<GeoEventPoint> queryGeoEventPoints(int limit) {
        return queryGeoEventPoints(limit, Set.of(), "", null, null);
    }

    /**
     * 按事件类型和国家过滤地理事件点，支持交互式地图筛选。
     */
    public List<GeoEventPoint> queryGeoEventPoints(int limit, Set<String> eventTypes, String countryCode) {
        return queryGeoEventPoints(limit, eventTypes, countryCode, null, null);
    }

    /**
     * 查询存在有效经纬度的事件日期，用于地图时间轴和窗口选择。
     */
    public List<LocalDate> queryGeoEventDates(Set<String> eventTypes, String countryCode) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT event_date
                FROM gdelt_events
                WHERE action_geo_lat IS NOT NULL
                  AND action_geo_lon IS NOT NULL
                  AND action_geo_lat BETWEEN -90 AND 90
                  AND action_geo_lon BETWEEN -180 AND 180
                """);
        List<String> parameters = appendGeoPointFilters(sql, eventTypes, countryCode, null, null);
        sql.append(" ORDER BY event_date");

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindStringParameters(statement, parameters, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<LocalDate> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(LocalDate.parse(resultSet.getString("event_date")));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("专题地图日期列表查询失败。", exception);
        }
    }

    /**
     * 查询指定时间窗口内的地理事件点，地图播放和窗口聚合都复用此方法。
     */
    public List<GeoEventPoint> queryGeoEventPoints(int limit, Set<String> eventTypes, String countryCode,
                                                   LocalDate startDate, LocalDate endDate) {
        StringBuilder filters = new StringBuilder("""
                FROM gdelt_events
                WHERE action_geo_lat IS NOT NULL
                  AND action_geo_lon IS NOT NULL
                  AND action_geo_lat BETWEEN -90 AND 90
                  AND action_geo_lon BETWEEN -180 AND 180
                """);
        List<String> parameters = appendGeoPointFilters(filters, eventTypes, countryCode, startDate, endDate);
        String sql = """
                WITH filtered AS (
                    SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                           event_type, action_geo_lat, action_geo_lon, goldstein_scale, avg_tone
                    %s
                ),
                ranked AS (
                    SELECT filtered.*,
                           ROW_NUMBER() OVER (PARTITION BY event_date ORDER BY global_event_id DESC) AS date_rank,
                           (SELECT COUNT(DISTINCT event_date) FROM filtered) AS date_count
                    FROM filtered
                )
                SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                       event_type, action_geo_lat, action_geo_lon, goldstein_scale, avg_tone
                FROM ranked
                WHERE date_rank <= MAX(1, (? + date_count - 1) / date_count)
                ORDER BY event_date DESC, global_event_id DESC
                LIMIT ?
                """.formatted(filters);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = bindStringParameters(statement, parameters, 1);
            int effectiveLimit = limit <= 0 ? 500 : limit;
            statement.setInt(parameterIndex++, effectiveLimit);
            statement.setInt(parameterIndex, effectiveLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GeoEventPoint> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapGeoEventPoint(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("专题地图事件点位查询失败。", exception);
        }
    }

    /**
     * 按日期预加载地理事件点序列，减少地图时间轴播放时的数据库往返。
     */
    public Map<LocalDate, List<GeoEventPoint>> queryGeoEventPointSeries(int limit, Set<String> eventTypes,
                                                                        String countryCode) {
        StringBuilder filters = new StringBuilder("""
                FROM gdelt_events
                WHERE action_geo_lat IS NOT NULL
                  AND action_geo_lon IS NOT NULL
                  AND action_geo_lat BETWEEN -90 AND 90
                  AND action_geo_lon BETWEEN -180 AND 180
                """);
        List<String> parameters = appendGeoPointFilters(filters, eventTypes, countryCode, null, null);
        String sql = """
                WITH filtered AS (
                    SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                           event_type, action_geo_lat, action_geo_lon, goldstein_scale, avg_tone
                    %s
                ),
                ranked AS (
                    SELECT filtered.*,
                           ROW_NUMBER() OVER (PARTITION BY event_date ORDER BY global_event_id DESC) AS date_rank
                    FROM filtered
                )
                SELECT global_event_id, event_date, actor1_country_code, actor2_country_code,
                       event_type, action_geo_lat, action_geo_lon, goldstein_scale, avg_tone
                FROM ranked
                WHERE date_rank <= ?
                ORDER BY event_date, global_event_id DESC
                """.formatted(filters);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = bindStringParameters(statement, parameters, 1);
            statement.setInt(parameterIndex, limit <= 0 ? 500 : limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<LocalDate, List<GeoEventPoint>> results = new LinkedHashMap<>();
                while (resultSet.next()) {
                    GeoEventPoint point = mapGeoEventPoint(resultSet);
                    results.computeIfAbsent(point.eventDate(), key -> new ArrayList<>()).add(point);
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("专题地图时间序列点位查询失败。", exception);
        }
    }

    /**
     * 追加地图查询的动态过滤条件，集中处理事件类型、国家和日期窗口。
     */
    private List<String> appendGeoPointFilters(StringBuilder sql, Set<String> eventTypes, String countryCode,
                                               LocalDate startDate, LocalDate endDate) {
        List<String> parameters = new ArrayList<>();
        if (eventTypes != null && !eventTypes.isEmpty()) {
            sql.append(" AND event_type IN (");
            int index = 0;
            for (String eventType : eventTypes) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                parameters.add(eventType);
                index++;
            }
            sql.append(")");
        }
        if (hasText(countryCode)) {
            sql.append(" AND (actor1_country_code = ? OR actor2_country_code = ?)");
            parameters.add(countryCode.trim().toUpperCase(Locale.ROOT));
            parameters.add(countryCode.trim().toUpperCase(Locale.ROOT));
        }
        if (startDate != null) {
            sql.append(" AND event_date >= ?");
            parameters.add(startDate.toString());
        }
        if (endDate != null) {
            sql.append(" AND event_date <= ?");
            parameters.add(endDate.toString());
        }
        return parameters;
    }

    private int bindStringParameters(PreparedStatement statement, List<String> parameters, int startIndex)
            throws SQLException {
        int parameterIndex = startIndex;
        for (String parameter : parameters) {
            statement.setString(parameterIndex++, parameter);
        }
        return parameterIndex;
    }

    /**
     * 将地理事件查询结果映射为地图点对象，隔离 ResultSet 字段读取细节。
     */
    private GeoEventPoint mapGeoEventPoint(ResultSet resultSet) throws SQLException {
        return new GeoEventPoint(
                resultSet.getString("global_event_id"),
                LocalDate.parse(resultSet.getString("event_date")),
                resultSet.getString("actor1_country_code"),
                resultSet.getString("actor2_country_code"),
                parseEventType(resultSet.getString("event_type")),
                resultSet.getDouble("action_geo_lat"),
                resultSet.getDouble("action_geo_lon"),
                resultSet.getDouble("goldstein_scale"),
                resultSet.getDouble("avg_tone")
        );
    }

    /**
     * 按区域聚合沿线国家指标，用于区域对比、雷达图和 XLSX 导出。
     */
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

    /**
     * 对区域合作/风险指数做归一化，确保不同区域之间可比较。
     */
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

    /**
     * 区域合作指数：综合区域事件规模、合作占比、媒体关注度和正向情绪。
     */
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

    /**
     * 区域风险指数：综合区域冲突规模、冲突占比、媒体关注度和负向情绪。
     */
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

    /**
     * 根据查询条件动态拼装 WHERE 子句，所有外部输入都通过参数绑定避免 SQL 注入。
     */
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

    private double cooperationIndex(CountryCooperationAggregate aggregate, int maxCooperationEvents, int maxMentions) {
        if (aggregate.totalEvents() <= 0) {
            return 0;
        }
        // 合作指数（course-display score）：综合合作占比、合作事件规模、
        // 正向 Goldstein、正向 AvgTone 和媒体关注度，并扣减冲突暴露。
        double cooperationRatio = ratio(aggregate.cooperationEvents(), aggregate.totalEvents());
        double conflictRatio = ratio(aggregate.conflictEvents(), aggregate.totalEvents());
        double cooperationShare = maxCooperationEvents <= 0 ? 0
                : (double) aggregate.cooperationEvents() / maxCooperationEvents;
        double mentionShare = maxMentions <= 0 ? 0 : (double) aggregate.totalMentions() / maxMentions;
        double positiveGoldstein = Math.max(aggregate.averageGoldstein(), 0) / 10.0;
        double positiveTone = Math.max(aggregate.averageAvgTone(), 0) / 100.0;
        return clamp(100.0 * (
                cooperationRatio * 0.35
                        + cooperationShare * 0.30
                        + positiveGoldstein * 0.15
                        + positiveTone * 0.08
                        + mentionShare * 0.12
                        - conflictRatio * 0.15
        ));
    }

    private double cooperationIndex(int cooperationEvents, int conflictEvents, double averageGoldstein,
                                    double averageAvgTone, int totalMentions) {
        int classifiedEvents = cooperationEvents + conflictEvents;
        if (classifiedEvents <= 0) {
            return 0;
        }
        // 双边/月度场景的合作指数：用于趋势和热点比较，权重更强调合作占比本身。
        double cooperationRatio = ratio(cooperationEvents, classifiedEvents);
        double conflictRatio = ratio(conflictEvents, classifiedEvents);
        double mentionSignal = Math.min(1.0, Math.log1p(Math.max(totalMentions, 0)) / 12.0);
        double positiveGoldstein = Math.max(averageGoldstein, 0) / 10.0;
        double positiveTone = Math.max(averageAvgTone, 0) / 100.0;
        return clamp(100.0 * (
                cooperationRatio * 0.55
                        + positiveGoldstein * 0.20
                        + positiveTone * 0.10
                        + mentionSignal * 0.15
                        - conflictRatio * 0.20
        ));
    }

    private double riskIndex(CountryRiskAggregate aggregate, double conflictRatio, int maxConflictEvents) {
        // 风险指数（risk score）：以冲突占比和冲突事件规模为主，
        // 负向 Goldstein 与负向 AvgTone 作为补充信号。
        double conflictShare = maxConflictEvents <= 0 ? 0
                : (double) aggregate.conflictEvents() / maxConflictEvents;
        double negativeGoldstein = Math.max(-aggregate.averageGoldstein(), 0) / 10.0;
        double negativeTone = Math.max(-aggregate.averageAvgTone(), 0) / 100.0;
        return clamp(100.0 * (
                conflictRatio * 0.35
                        + conflictShare * 0.45
                        + negativeGoldstein * 0.12
                        + negativeTone * 0.08
        ));
    }

    private double riskIndex(int conflictEvents, double conflictRatio, double averageGoldstein, double averageAvgTone) {
        // 热点/月度风险指数：平衡冲突占比、事件规模和负面情绪信号。
        double eventSignal = Math.min(1.0, Math.log1p(Math.max(conflictEvents, 0)) / 10.0);
        double negativeGoldstein = Math.max(-averageGoldstein, 0) / 10.0;
        double negativeTone = Math.max(-averageAvgTone, 0) / 100.0;
        return clamp(100.0 * (
                conflictRatio * 0.45
                        + eventSignal * 0.35
                        + negativeGoldstein * 0.12
                        + negativeTone * 0.08
        ));
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

    private String normalizeRegion(String region) {
        return region == null || region.isBlank() ? "其他" : region;
    }

    private record CountryCooperationAggregate(String countryCode, String countryName, String region,
                                               int totalEvents, int cooperationEvents, int conflictEvents,
                                               double averageGoldstein, double averageAvgTone, int totalMentions) {
    }

    private record CountryRiskAggregate(String countryCode, int totalEvents, int conflictEvents,
                                        double averageGoldstein, double averageAvgTone) {
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
