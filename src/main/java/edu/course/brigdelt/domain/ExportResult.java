package edu.course.brigdelt.domain;

import java.nio.file.Path;
import java.util.List;

public record ExportResult(
        List<Path> files,
        long durationMillis
) {
    public String displaySummary() {
        return "已生成 " + files.size() + " 个文件，用时 " + durationMillis + " ms";
    }
}
