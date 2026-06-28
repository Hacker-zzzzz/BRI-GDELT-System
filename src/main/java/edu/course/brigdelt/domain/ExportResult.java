package edu.course.brigdelt.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * 报告导出结果，记录生成的文件列表和耗时，用于 UI 任务完成提示。
 */
public record ExportResult(
        List<Path> files,
        long durationMillis
) {
    public String displaySummary() {
        return "已生成 " + files.size() + " 个文件，用时 " + durationMillis + " ms";
    }
}
