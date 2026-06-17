package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.ExportResult;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.repository.DatabaseManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportExportService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AppPaths paths;
    private final DashboardService dashboardService;
    private final AnalysisService analysisService;

    public ReportExportService(AppPaths paths) {
        this.paths = paths;
        DatabaseManager databaseManager = new DatabaseManager(paths);
        this.dashboardService = new DashboardService(databaseManager);
        this.analysisService = new AnalysisService(databaseManager);
    }

    public ExportResult exportSnapshot() {
        Instant start = Instant.now();
        try {
            Files.createDirectories(paths.reportDir());
            Files.createDirectories(paths.exportDir());

            DashboardSummary summary = dashboardService.loadSummary();
            List<CooperationScore> cooperationScores = analysisService.cooperationRankings(30);
            List<RiskAssessment> riskAssessments = analysisService.riskRankings(30);
            String suffix = LocalDateTime.now().format(FILE_TIME_FORMAT);

            List<Path> files = new ArrayList<>();
            Path reportFile = paths.reportDir().resolve("bri_gdelt_snapshot_" + suffix + ".txt");
            Files.writeString(reportFile, buildReport(summary, cooperationScores, riskAssessments),
                    StandardCharsets.UTF_8);
            files.add(reportFile);

            Path cooperationFile = paths.exportDir().resolve("cooperation_rankings_" + suffix + ".csv");
            Files.writeString(cooperationFile, buildCooperationCsv(cooperationScores), StandardCharsets.UTF_8);
            files.add(cooperationFile);

            Path riskFile = paths.exportDir().resolve("risk_rankings_" + suffix + ".csv");
            Files.writeString(riskFile, buildRiskCsv(riskAssessments), StandardCharsets.UTF_8);
            files.add(riskFile);

            return new ExportResult(files, Duration.between(start, Instant.now()).toMillis());
        } catch (IOException exception) {
            throw new IllegalStateException("导出分析结果失败。", exception);
        }
    }

    private String buildReport(DashboardSummary summary, List<CooperationScore> cooperationScores,
                               List<RiskAssessment> riskAssessments) {
        String topCooperation = cooperationScores.isEmpty() ? "暂无" : cooperationScores.get(0).countryCode();
        String topRisk = riskAssessments.isEmpty() ? "暂无" : riskAssessments.get(0).countryCode();
        double cooperationRatio = summary.totalEvents() == 0 ? 0 : (double) summary.cooperationEvents() / summary.totalEvents();
        double conflictRatio = summary.totalEvents() == 0 ? 0 : (double) summary.conflictEvents() / summary.totalEvents();
        return """
                一带一路沿线国家合作态势分析系统
                导出时间：%s

                一、总体概况
                配置国家数：%d
                GDELT 事件总量：%d
                合作事件：%d（%.1f%%）
                冲突事件：%d（%.1f%%）
                其他事件：%d
                导入批次：%d
                媒体关注度 NumMentions：%d
                平均 Goldstein：%.2f
                平均 AvgTone：%.2f

                二、重点研判
                合作指数首位国家：%s
                风险指数首位国家：%s

                三、导出文件说明
                cooperation_rankings_*.csv：国家合作指数排名
                risk_rankings_*.csv：国家风险指数排名
                """.formatted(
                LocalDateTime.now(),
                summary.countryCount(),
                summary.totalEvents(),
                summary.cooperationEvents(),
                cooperationRatio * 100,
                summary.conflictEvents(),
                conflictRatio * 100,
                summary.otherEvents(),
                summary.importBatches(),
                summary.totalMentions(),
                summary.averageGoldstein(),
                summary.averageAvgTone(),
                topCooperation,
                topRisk
        );
    }

    private String buildCooperationCsv(List<CooperationScore> scores) {
        StringBuilder builder = new StringBuilder("country,total_events,cooperation_events,conflict_events,average_goldstein,average_avg_tone,total_mentions,cooperation_index\n");
        for (CooperationScore score : scores) {
            builder.append(score.countryCode()).append(',')
                    .append(score.totalEvents()).append(',')
                    .append(score.cooperationEvents()).append(',')
                    .append(score.conflictEvents()).append(',')
                    .append("%.4f".formatted(score.averageGoldstein())).append(',')
                    .append("%.4f".formatted(score.averageAvgTone())).append(',')
                    .append(score.totalMentions()).append(',')
                    .append("%.4f".formatted(score.cooperationIndex())).append('\n');
        }
        return builder.toString();
    }

    private String buildRiskCsv(List<RiskAssessment> risks) {
        StringBuilder builder = new StringBuilder("country,total_events,conflict_events,conflict_ratio,average_goldstein,average_avg_tone,risk_index,risk_level\n");
        for (RiskAssessment risk : risks) {
            builder.append(risk.countryCode()).append(',')
                    .append(risk.totalEvents()).append(',')
                    .append(risk.conflictEvents()).append(',')
                    .append("%.4f".formatted(risk.conflictRatio())).append(',')
                    .append("%.4f".formatted(risk.averageGoldstein())).append(',')
                    .append("%.4f".formatted(risk.averageAvgTone())).append(',')
                    .append("%.4f".formatted(risk.riskIndex())).append(',')
                    .append(risk.riskLevel()).append('\n');
        }
        return builder.toString();
    }
}
