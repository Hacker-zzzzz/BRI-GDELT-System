package edu.course.brigdelt.repository;

import edu.course.brigdelt.config.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns SQLite connection creation and first-run schema initialization.
 */
public class DatabaseManager {

    private final AppPaths paths;

    public DatabaseManager(AppPaths paths) {
        this.paths = paths;
    }

    public void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
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
                        is_bri_country INTEGER NOT NULL DEFAULT 1
                    )
                    """);
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
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_date ON gdelt_events(event_date)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_actor1 ON gdelt_events(actor1_country_code)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_actor2 ON gdelt_events(actor2_country_code)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_type ON gdelt_events(event_type)");
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_events_bilateral_date
                    ON gdelt_events(actor1_country_code, actor2_country_code, event_date)
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite 数据库初始化失败：" + paths.databaseFile(), exception);
        }
    }

    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + paths.databaseFile().toAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
