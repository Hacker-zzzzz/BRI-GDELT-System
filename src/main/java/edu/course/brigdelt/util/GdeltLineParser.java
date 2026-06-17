package edu.course.brigdelt.util;

import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.GdeltEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Safe parser for tab-separated GDELT event lines.
 */
public final class GdeltLineParser {
    private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private GdeltLineParser() {
    }

    public static Optional<GdeltEvent> parse(String line, String sourceFile) {
        return parseDetailed(line, sourceFile).event();
    }

    public static ParseResult parseDetailed(String line, String sourceFile) {
        if (line == null || line.isBlank()) {
            return ParseResult.failure("行内容为空");
        }

        String[] fields = line.split("\t", -1);
        if (fields.length < GdeltFieldMapper.MIN_REQUIRED_FIELD_COUNT) {
            return ParseResult.failure("字段数量不足：期望至少 %d 列，实际 %d 列"
                    .formatted(GdeltFieldMapper.MIN_REQUIRED_FIELD_COUNT, fields.length));
        }

        try {
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
