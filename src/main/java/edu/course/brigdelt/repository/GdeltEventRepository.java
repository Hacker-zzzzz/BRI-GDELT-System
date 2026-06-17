package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.GdeltEvent;
import edu.course.brigdelt.domain.MonthlyTrendPoint;

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
