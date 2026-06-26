package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.CountryClusterResult;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.ExportResult;
import edu.course.brigdelt.domain.RegionSummary;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.repository.DatabaseManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            List<CooperationScore> cooperationScores = analysisService.cooperationRankings(AnalysisService.DEFAULT_RANK_LIMIT);
            List<RiskAssessment> riskAssessments = analysisService.riskRankings(AnalysisService.DEFAULT_RANK_LIMIT);
            List<RegionSummary> regionSummaries = analysisService.regionSummaries();
            List<CountryClusterResult> clusterResults = analysisService.countryClusters();
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

            Path workbookFile = paths.exportDir().resolve("bri_gdelt_analysis_" + suffix + ".xlsx");
            writeWorkbook(workbookFile, cooperationScores, riskAssessments, regionSummaries, clusterResults);
            files.add(workbookFile);

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
                合作指数口径：合作事件量、正向 Goldstein、正向 AvgTone、媒体关注度加权，扣除冲突事件。
                风险指数口径：冲突占比、负向 Goldstein、负向 AvgTone 和冲突事件量加权。
                聚类口径：k=4，使用合作指数、风险指数、冲突占比、Goldstein、AvgTone 和事件量归一化。

                三、导出文件说明
                cooperation_rankings_*.csv：国家合作指数排名
                risk_rankings_*.csv：国家风险指数排名
                bri_gdelt_analysis_*.xlsx：合作、风险、区域汇总、国家聚类四个工作表
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

    private void writeWorkbook(Path workbookFile, List<CooperationScore> cooperationScores,
                               List<RiskAssessment> riskAssessments,
                               List<RegionSummary> regionSummaries,
                               List<CountryClusterResult> clusterResults) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(workbookFile);
             ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            addZipEntry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            addZipEntry(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            addZipEntry(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="合作排名" sheetId="1" r:id="rId1"/>
                        <sheet name="风险排名" sheetId="2" r:id="rId2"/>
                        <sheet name="区域汇总" sheetId="3" r:id="rId3"/>
                        <sheet name="国家聚类" sheetId="4" r:id="rId4"/>
                      </sheets>
                    </workbook>
                    """);
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                      <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                      <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                    </Relationships>
                    """);
            addZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(cooperationRows(cooperationScores)));
            addZipEntry(zip, "xl/worksheets/sheet2.xml", worksheetXml(riskRows(riskAssessments)));
            addZipEntry(zip, "xl/worksheets/sheet3.xml", worksheetXml(regionRows(regionSummaries)));
            addZipEntry(zip, "xl/worksheets/sheet4.xml", worksheetXml(clusterRows(clusterResults)));
        }
    }

    private List<List<Object>> cooperationRows(List<CooperationScore> scores) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("country", "total_events", "cooperation_events", "conflict_events", "average_goldstein", "average_avg_tone", "total_mentions", "cooperation_index"));
        for (CooperationScore score : scores) {
            rows.add(List.of(score.countryCode(), score.totalEvents(), score.cooperationEvents(), score.conflictEvents(),
                    score.averageGoldstein(), score.averageAvgTone(), score.totalMentions(), score.cooperationIndex()));
        }
        return rows;
    }

    private List<List<Object>> riskRows(List<RiskAssessment> risks) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("country", "total_events", "conflict_events", "conflict_ratio", "average_goldstein", "average_avg_tone", "risk_index", "risk_level"));
        for (RiskAssessment risk : risks) {
            rows.add(List.of(risk.countryCode(), risk.totalEvents(), risk.conflictEvents(), risk.conflictRatio(),
                    risk.averageGoldstein(), risk.averageAvgTone(), risk.riskIndex(), risk.riskLevel()));
        }
        return rows;
    }

    private List<List<Object>> regionRows(List<RegionSummary> regions) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("region", "country_count", "total_events", "cooperation_events", "conflict_events", "average_goldstein", "average_avg_tone", "total_mentions", "cooperation_index", "risk_index"));
        for (RegionSummary region : regions) {
            rows.add(List.of(region.region(), region.countryCount(), region.totalEvents(), region.cooperationEvents(),
                    region.conflictEvents(), region.averageGoldstein(), region.averageAvgTone(), region.totalMentions(),
                    region.cooperationIndex(), region.riskIndex()));
        }
        return rows;
    }

    private List<List<Object>> clusterRows(List<CountryClusterResult> clusters) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("country", "name", "region", "total_events", "cooperation_index", "risk_index", "conflict_ratio", "cluster_label", "explanation"));
        for (CountryClusterResult cluster : clusters) {
            rows.add(List.of(cluster.countryCode(), cluster.countryName(), cluster.region(), cluster.totalEvents(),
                    cluster.cooperationIndex(), cluster.riskIndex(), cluster.conflictRatio(),
                    cluster.clusterLabel(), cluster.explanation()));
        }
        return rows;
    }

    private String worksheetXml(List<List<Object>> rows) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("    <row r=\"").append(rowIndex + 1).append("\">\n");
            List<Object> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String cellRef = columnName(columnIndex) + (rowIndex + 1);
                appendCell(builder, cellRef, row.get(columnIndex));
            }
            builder.append("    </row>\n");
        }
        builder.append("  </sheetData>\n</worksheet>\n");
        return builder.toString();
    }

    private void appendCell(StringBuilder builder, String cellRef, Object value) {
        if (value instanceof Number number) {
            builder.append("      <c r=\"").append(cellRef).append("\"><v>")
                    .append(number).append("</v></c>\n");
            return;
        }
        builder.append("      <c r=\"").append(cellRef).append("\" t=\"inlineStr\"><is><t>")
                .append(xmlEscape(String.valueOf(value)))
                .append("</t></is></c>\n");
    }

    private String columnName(int index) {
        StringBuilder builder = new StringBuilder();
        int value = index;
        do {
            builder.insert(0, (char) ('A' + value % 26));
            value = value / 26 - 1;
        } while (value >= 0);
        return builder.toString();
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
