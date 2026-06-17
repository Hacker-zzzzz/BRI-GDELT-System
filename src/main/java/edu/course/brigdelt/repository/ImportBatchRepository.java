package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.ImportBatchStatus;
import edu.course.brigdelt.domain.ImportResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists GDELT file import batch summaries.
 */
public class ImportBatchRepository {

    private final DatabaseManager databaseManager;

    public ImportBatchRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int insert(ImportResult result) {
        return insert(
                result.fileName(),
                result.totalRows(),
                result.successRows(),
                result.skippedRows(),
                result.status(),
                result.errorSummary()
        );
    }

    public int insert(String fileName, int totalRows, int successRows, int skippedRows,
                      ImportBatchStatus status, String message) {
        String sql = """
                INSERT INTO import_batches (
                    file_name,
                    total_rows,
                    success_rows,
                    skipped_rows,
                    status,
                    message
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileName);
            statement.setInt(2, totalRows);
            statement.setInt(3, successRows);
            statement.setInt(4, skippedRows);
            statement.setString(5, status == null ? null : status.name());
            statement.setString(6, message);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("导入批次记录入库失败。", exception);
        }
    }

    public int countImportBatches() {
        String sql = "SELECT COUNT(*) FROM import_batches";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("统计导入批次数量失败。", exception);
        }
    }
}
