package com.wonkglorg.doc.core.objects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility Method to properly format and parse dates to / from the database
 */
public class DateHelper {
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Parse a date time string into a LocalDateTime object
     * @param dateTime the date time string
     * @return the parsed date time object
     */
    public static LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null) {
            return null;
        }
        return LocalDateTime.parse(dateTime, formatter);
    }

    /**
     * Format a LocalDateTime object into a date time string
     * @param dateTime the date time object
     * @return the formatted date time string
     */
    public static String fromDateTime(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }


}
