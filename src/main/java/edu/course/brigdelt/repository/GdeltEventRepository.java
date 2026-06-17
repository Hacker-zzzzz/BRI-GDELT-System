package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.GdeltEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
}
