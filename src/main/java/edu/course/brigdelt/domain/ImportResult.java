package edu.course.brigdelt.domain;

import java.util.List;

/**
 * Summary of one GDELT import batch.
 */
public record ImportResult(
        String fileName,
        int totalRows,
        int successRows,
        int skippedRows,
        int insertedRows,
        ImportBatchStatus status,
        String errorSummary,
        long durationMillis,
        List<String> errorSamples
) {
    public ImportResult {
        errorSamples = errorSamples == null ? List.of() : List.copyOf(errorSamples);
    }

    public String displaySummary() {
        String statusText = switch (status) {
            case SUCCESS -> "成功";
            case FAILED -> "失败";
            case PARTIAL -> "部分成功";
        };
        return "%s：总行数 %d，成功 %d，跳过 %d，入库 %d，耗时 %d ms"
                .formatted(statusText, totalRows, successRows, skippedRows, insertedRows, durationMillis);
    }

    public boolean hasErrors() {
        return status != ImportBatchStatus.SUCCESS || !errorSamples.isEmpty();
    }
}
