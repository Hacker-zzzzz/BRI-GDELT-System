package edu.course.brigdelt.util;

/**
 * Zero-based Java array indexes for GDELT 2.0 Events export fields.
 *
 * GDELT documentation commonly numbers fields from 1. These constants are
 * already converted to Java's 0-based array indexes.
 */
public final class GdeltFieldMapper {
    public static final int GLOBAL_EVENT_ID_INDEX = 0;
    public static final int SQL_DATE_INDEX = 1;
    public static final int ACTOR1_COUNTRY_CODE_INDEX = 7;
    public static final int ACTOR2_COUNTRY_CODE_INDEX = 17;
    public static final int EVENT_CODE_INDEX = 26;
    public static final int EVENT_BASE_CODE_INDEX = 27;
    public static final int EVENT_ROOT_CODE_INDEX = 28;
    public static final int GOLDSTEIN_SCALE_INDEX = 30;
    public static final int NUM_MENTIONS_INDEX = 31;
    public static final int AVG_TONE_INDEX = 34;
    public static final int ACTION_GEO_LAT_INDEX = 56;
    public static final int ACTION_GEO_LONG_INDEX = 57;

    public static final int MIN_REQUIRED_FIELD_COUNT = ACTION_GEO_LONG_INDEX + 1;

    private GdeltFieldMapper() {
    }
}
