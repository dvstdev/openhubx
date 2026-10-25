package com.darktile.openhubx.dao;

import androidx.room.TypeConverter;

import java.util.Date;

/** Maps {@link Date} to the epoch-millis INTEGER columns greenDAO used. */
public class DateConverter {

    @TypeConverter
    public static Date toDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long toLong(Date date) {
        return date == null ? null : date.getTime();
    }
}
