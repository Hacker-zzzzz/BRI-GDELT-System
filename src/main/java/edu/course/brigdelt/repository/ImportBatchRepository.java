package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.ImportBatchStatus;
import edu.course.brigdelt.domain.ImportResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 导入批次仓储，负责记录每个 GDELT 文件的导入结果和错误摘要。
 *
 * <p>这些记录用于启动自检、导入页面反馈和后续排查数据质量问题。</p>
 */
public class ImportBatchRepository {

    private final DatabaseManager databaseManager;

    public ImportBatchRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 持久化导入服务返回的批次结果，保持 UI 层不直接接触数据库字段。
     */
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

    /**
     * 插入一条导入批次记录，记录总行数、成功数、跳过数和状态信息。
     */
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

    /**
     * 统计已记录的导入批次数，用于命令行自检确认数据库表可读。
     */
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
