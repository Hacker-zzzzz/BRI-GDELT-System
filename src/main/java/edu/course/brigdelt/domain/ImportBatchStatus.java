package edu.course.brigdelt.domain;

/**
 * GDELT 文件导入批次状态，用于区分完全成功、失败和部分成功三种结果。
 */
public enum ImportBatchStatus {
    SUCCESS,
    FAILED,
    PARTIAL
}
