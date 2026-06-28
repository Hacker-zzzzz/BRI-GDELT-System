package edu.course.brigdelt.repository;

import edu.course.brigdelt.config.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 数据库管理器。
 *
 * <p>集中负责连接创建、首次表结构初始化、兼容旧库的补列逻辑和连接级 PRAGMA 设置。</p>
 */
public class DatabaseManager {

    private final AppPaths paths;

    public DatabaseManager(AppPaths paths) {
        this.paths = paths;
    }

    /**
     * 初始化应用所需表和索引。方法可重复执行，适合每次启动时做安全检查。
     */
    public void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA journal_mode=WAL");
            // 国家表保存 CAMEO/ISO 代码、中文名、英文名、区域和地图中心点。
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS countries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        cameo_code TEXT NOT NULL UNIQUE,
                        iso_code TEXT,
                        name_cn TEXT NOT NULL,
                        name_en TEXT NOT NULL,
                        region TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        is_bri_country INTEGER NOT NULL DEFAULT 1,
                        is_core_country INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            addColumnIfMissing(connection, "countries", "iso_code", "TEXT");
            addColumnIfMissing(connection, "countries", "name_cn", "TEXT");
            addColumnIfMissing(connection, "countries", "name_en", "TEXT");
            addColumnIfMissing(connection, "countries", "region", "TEXT");
            addColumnIfMissing(connection, "countries", "latitude", "REAL");
            addColumnIfMissing(connection, "countries", "longitude", "REAL");
            addColumnIfMissing(connection, "countries", "is_bri_country", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfMissing(connection, "countries", "is_core_country", "INTEGER NOT NULL DEFAULT 0");

            // 事件表只保存本系统分析需要的 GDELT 核心字段，避免原始宽表过重。
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gdelt_events (
                        global_event_id TEXT PRIMARY KEY,
                        event_date TEXT NOT NULL,
                        actor1_country_code TEXT,
                        actor2_country_code TEXT,
                        event_code TEXT,
                        event_base_code TEXT,
                        event_root_code TEXT,
                        event_type TEXT,
                        goldstein_scale REAL,
                        num_mentions INTEGER,
                        avg_tone REAL,
                        action_geo_lat REAL,
                        action_geo_lon REAL,
                        source_file TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            addColumnIfMissing(connection, "gdelt_events", "event_date", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "actor1_country_code", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "actor2_country_code", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "event_code", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "event_base_code", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "event_root_code", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "event_type", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "goldstein_scale", "REAL");
            addColumnIfMissing(connection, "gdelt_events", "num_mentions", "INTEGER");
            addColumnIfMissing(connection, "gdelt_events", "avg_tone", "REAL");
            addColumnIfMissing(connection, "gdelt_events", "action_geo_lat", "REAL");
            addColumnIfMissing(connection, "gdelt_events", "action_geo_lon", "REAL");
            addColumnIfMissing(connection, "gdelt_events", "source_file", "TEXT");
            addColumnIfMissing(connection, "gdelt_events", "created_at", "TEXT DEFAULT CURRENT_TIMESTAMP");

            // 导入批次表用于首页展示导入历史，也用于缓存版本判断。
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS import_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_name TEXT NOT NULL,
                        import_time TEXT DEFAULT CURRENT_TIMESTAMP,
                        total_rows INTEGER NOT NULL DEFAULT 0,
                        success_rows INTEGER NOT NULL DEFAULT 0,
                        skipped_rows INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL,
                        message TEXT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS analysis_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        analysis_type TEXT NOT NULL,
                        country_code TEXT,
                        region TEXT,
                        period TEXT,
                        score REAL,
                        risk_level TEXT,
                        detail_json TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            // 关键索引覆盖日期、国家、事件类型和地图查询，提升课堂演示时的响应速度。
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_countries_cameo_code ON countries(cameo_code)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_date ON gdelt_events(event_date)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_actor1 ON gdelt_events(actor1_country_code)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_actor2 ON gdelt_events(actor2_country_code)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_type ON gdelt_events(event_type)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_root_code ON gdelt_events(event_root_code)");
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_bilateral_date
                    ON gdelt_events(actor1_country_code, actor2_country_code, event_date)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_date_id_desc
                    ON gdelt_events(event_date DESC, global_event_id DESC)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_type_date_id
                    ON gdelt_events(event_type, event_date DESC, global_event_id DESC)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_actor1_date_id
                    ON gdelt_events(actor1_country_code, event_date DESC, global_event_id DESC)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_actor2_date_id
                    ON gdelt_events(actor2_country_code, event_date DESC, global_event_id DESC)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_bilateral_date_id
                    ON gdelt_events(actor1_country_code, actor2_country_code, event_date DESC, global_event_id DESC)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_month_actor1
                    ON gdelt_events(substr(event_date, 1, 7), actor1_country_code)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_month_actor2
                    ON gdelt_events(substr(event_date, 1, 7), actor2_country_code)
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_source_day
                    ON gdelt_events(substr(source_file, 1, 8))
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_geo_date
                    ON gdelt_events(event_date, global_event_id DESC)
                    WHERE action_geo_lat IS NOT NULL AND action_geo_lon IS NOT NULL
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite 数据库表结构初始化失败：" + paths.databaseFile(), exception);
        }
    }

    /**
     * 创建 SQLite 连接并应用统一 PRAGMA 配置。
     */
    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + paths.databaseFile().toAbsolutePath();
        Connection connection = DriverManager.getConnection(url);
        configureConnection(connection);
        return connection;
    }

    public boolean tableExists(String tableName) {
        String sql = """
                SELECT 1
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """;
        try (Connection connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite 数据库表结构检查失败：" + paths.databaseFile(), exception);
        }
    }

    /**
     * 兼容旧版本数据库：字段不存在时补列，避免用户已有数据因升级丢失。
     */
    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String columnDefinition)
            throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 设置连接级性能参数，平衡 SQLite 写入性能和桌面应用稳定性。
     */
    private void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA synchronous=NORMAL");
            statement.executeUpdate("PRAGMA temp_store=MEMORY");
            statement.executeUpdate("PRAGMA cache_size=-65536");
            statement.executeUpdate("PRAGMA busy_timeout=5000");
        }
    }
}
