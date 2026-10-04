package com.printxpress.app.data.local;

import androidx.room.TypeConverter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Converters {

    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    private static final String ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        if (value == null) return null;
        Calendar utcCal = Calendar.getInstance(UTC_TIMEZONE);
        utcCal.setTimeInMillis(value);
        return utcCal.getTime();
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        if (date == null) return null;
        Calendar utcCal = Calendar.getInstance(UTC_TIMEZONE);
        utcCal.setTime(date);
        return utcCal.getTimeInMillis();
    }

    public static String formatUtcIso(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_UTC_PATTERN, Locale.US);
        sdf.setTimeZone(UTC_TIMEZONE);
        return sdf.format(date);
    }

    public static Date parseUtcIso(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_UTC_PATTERN, Locale.US);
        sdf.setTimeZone(UTC_TIMEZONE);
        try {
            return sdf.parse(isoString.trim());
        } catch (ParseException e) {
            return null;
        }
    }
}

