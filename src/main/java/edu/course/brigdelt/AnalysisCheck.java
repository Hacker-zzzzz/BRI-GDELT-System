package edu.course.brigdelt;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.ExportResult;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.service.AnalysisService;
import edu.course.brigdelt.service.ReportExportService;
import edu.course.brigdelt.service.StartupService;
import edu.course.brigdelt.util.GdeltLineParser;

import java.nio.file.Files;
import java.util.Arrays;

/**
 * Console smoke check for parser, analysis aggregation, clustering and export.
 */
public final class AnalysisCheck {

    private AnalysisCheck() {
    }

    public static void main(String[] args) {
        AppPaths paths = new StartupService().initialize();
        AnalysisService analysisService = new AnalysisService(new DatabaseManager(paths));

        assertParserAndEventType();
        int regionCount = analysisService.regionSummaries().size();
        int clusterCount = analysisService.countryClusters().size();
        if (regionCount == 0) {
            throw new IllegalStateException("区域汇总为空。");
        }
        if (clusterCount == 0) {
            throw new IllegalStateException("国家聚类结果为空。");
        }

        ExportResult exportResult = new ReportExportService(paths).exportSnapshot();
        boolean hasWorkbook = exportResult.files().stream()
                .anyMatch(path -> path.getFileName().toString().endsWith(".xlsx") && Files.isRegularFile(path));
        if (!hasWorkbook) {
            throw new IllegalStateException("导出结果未包含 XLSX 工作簿。");
        }

        System.out.println("区域汇总数量: " + regionCount);
        System.out.println("聚类国家数量: " + clusterCount);
        System.out.println("导出文件数量: " + exportResult.files().size());
        exportResult.files().forEach(path -> System.out.println("导出文件: " + path));
        System.out.println("分析自检通过。");
    }

    private static void assertParserAndEventType() {
        String[] fields = new String[58];
        Arrays.fill(fields, "");
        fields[0] = "10000001";
        fields[1] = "20250601";
        fields[7] = "CHN";
        fields[17] = "PAK";
        fields[26] = "043";
        fields[27] = "043";
        fields[28] = "04";
        fields[30] = "5.0";
        fields[31] = "12";
        fields[34] = "1.5";
        fields[56] = "30.0";
        fields[57] = "70.0";
        GdeltLineParser.ParseResult result = GdeltLineParser.parseDetailed(String.join("\t", fields), "analysis-check.tsv");
        if (!result.success() || result.event().orElseThrow().eventType() != EventType.COOPERATION) {
            throw new IllegalStateException("GDELT 解析或合作事件分类自检失败。");
        }
        if (EventType.fromRootCode("14") != EventType.CONFLICT) {
            throw new IllegalStateException("冲突事件分类自检失败。");
        }
    }
}
