package edu.course.brigdelt.util;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.GdeltEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * GDELT 原始 TSV 行解析器，负责把固定列位置的事件字段转换为系统领域对象。
 *
 * <p>解析失败不会直接抛到导入主流程，而是返回错误信息，便于导入服务收集错误样例。</p>
 */
public final class GdeltLineParser {
    private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private GdeltLineParser() {
    }

    /**
     * 简化解析入口，只关心是否能得到有效事件记录。
     */
    public static Optional<GdeltEvent> parse(String line, String sourceFile) {
        return parseDetailed(line, sourceFile).event();
    }

    /**
     * 详细解析入口，保留失败原因，供导入统计和错误样例展示使用。
     */
    public static ParseResult parseDetailed(String line, String sourceFile) {
        if (line == null || line.isBlank()) {
            return ParseResult.failure("行内容为空");
        }

        // GDELT Export CSV 实际为 Tab 分隔，-1 保留末尾空列，避免字段位置整体偏移。
        String[] fields = line.split("\t", -1);
        if (fields.length < GdeltFieldMapper.MIN_REQUIRED_FIELD_COUNT) {
            return ParseResult.failure("字段数量不足：期望至少 %d 列，实际 %d 列"
                    .formatted(GdeltFieldMapper.MIN_REQUIRED_FIELD_COUNT, fields.length));
        }

        try {
            // 只抽取课程分析需要的核心列：主体国家、CAMEO 编码、强度、情绪和地理坐标。
            long globalEventId = parseLong(fields, GdeltFieldMapper.GLOBAL_EVENT_ID_INDEX, "GlobalEventID");
            LocalDate eventDate = parseSqlDate(fields, GdeltFieldMapper.SQL_DATE_INDEX, "SQLDATE");
            String actor1CountryCode = trimToNull(fields[GdeltFieldMapper.ACTOR1_COUNTRY_CODE_INDEX]);
            String actor2CountryCode = trimToNull(fields[GdeltFieldMapper.ACTOR2_COUNTRY_CODE_INDEX]);
            String eventCode = trimToEmpty(fields[GdeltFieldMapper.EVENT_CODE_INDEX]);
            String eventBaseCode = trimToEmpty(fields[GdeltFieldMapper.EVENT_BASE_CODE_INDEX]);
            String eventRootCode = trimToEmpty(fields[GdeltFieldMapper.EVENT_ROOT_CODE_INDEX]);
            double goldsteinScale = parseDouble(fields, GdeltFieldMapper.GOLDSTEIN_SCALE_INDEX, "GoldsteinScale");
            int numMentions = parseInt(fields, GdeltFieldMapper.NUM_MENTIONS_INDEX, "NumMentions");
            double avgTone = parseDouble(fields, GdeltFieldMapper.AVG_TONE_INDEX, "AvgTone");
            Double actionGeoLat = parseNullableDouble(fields, GdeltFieldMapper.ACTION_GEO_LAT_INDEX, "ActionGeo_Lat");
            Double actionGeoLon = parseNullableDouble(fields, GdeltFieldMapper.ACTION_GEO_LONG_INDEX, "ActionGeo_Long");

            GdeltEvent event = new GdeltEvent(
                    globalEventId,
                    eventDate,
                    actor1CountryCode,
                    actor2CountryCode,
                    eventCode,
                    eventBaseCode,
                    eventRootCode,
                    EventType.fromRootCode(eventRootCode),
                    goldsteinScale,
                    numMentions,
                    avgTone,
                    actionGeoLat,
                    actionGeoLon,
                    trimToNull(sourceFile)
            );
            return ParseResult.success(event);
        } catch (IllegalArgumentException ex) {
            return ParseResult.failure(ex.getMessage());
        }
    }

    /**
     * 解析必填长整数字段，错误信息带字段名，便于定位原始数据问题。
     */
    private static long parseLong(String[] fields, int index, String fieldName) {
        String value = trimToEmpty(fields[index]);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("字段 %s 不能为空".formatted(fieldName));
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段 %s 数字格式错误：%s".formatted(fieldName, value));
        }
    }

    /**
     * 解析必填整数字段，例如 NumMentions。
     */
    private static int parseInt(String[] fields, int index, String fieldName) {
        String value = trimToEmpty(fields[index]);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("字段 %s 不能为空".formatted(fieldName));
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段 %s 整数格式错误：%s".formatted(fieldName, value));
        }
    }

    /**
     * 解析必填小数字段，例如 GoldsteinScale 和 AvgTone。
     */
    private static double parseDouble(String[] fields, int index, String fieldName) {
        String value = trimToEmpty(fields[index]);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("字段 %s 不能为空".formatted(fieldName));
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段 %s 小数格式错误：%s".formatted(fieldName, value));
        }
    }

    /**
     * 解析可为空的经纬度字段；缺失坐标不影响事件入库，只影响地图展示。
     */
    private static Double parseNullableDouble(String[] fields, int index, String fieldName) {
        String value = trimToEmpty(fields[index]);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段 %s 小数格式错误：%s".formatted(fieldName, value));
        }
    }

    /**
     * 解析 GDELT SQLDATE，原始格式为 yyyyMMdd。
     */
    private static LocalDate parseSqlDate(String[] fields, int index, String fieldName) {
        String value = trimToEmpty(fields[index]);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("字段 %s 不能为空".formatted(fieldName));
        }
        try {
            return LocalDate.parse(value, SQL_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("字段 %s 日期格式错误，应为 yyyyMMdd：%s".formatted(fieldName, value));
        }
    }

    /**
     * 清理 BOM 和首尾空白，减少原始文件编码差异带来的解析失败。
     */
    private static String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "").trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 单行解析结果，成功时携带事件对象，失败时携带可展示的错误摘要。
     */
    public record ParseResult(Optional<GdeltEvent> event, String errorMessage) {
        public ParseResult {
            event = event == null ? Optional.empty() : event;
        }

        public static ParseResult success(GdeltEvent event) {
            return new ParseResult(Optional.of(event), null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(Optional.empty(), errorMessage);
        }

        public boolean success() {
            return event.isPresent();
        }
    }
}
