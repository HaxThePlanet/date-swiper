package com.tinderizer.utils;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
//    public static Date getTimeUntilMidnight() {
//        Calendar midnight = Calendar.getInstance();
//        midnight.setTime(new Date());
//        midnight.set(Calendar.HOUR_OF_DAY, 23);
//        midnight.set(Calendar.MINUTE, 59);
//        midnight.set(Calendar.SECOND, 59);
//        midnight.set(Calendar.MILLISECOND, 999);
//
//        Date now = new Date();
//        now.getTime();
//
//        Date utilDate = midnight.getTime();
//
//
//        return (int) (now.getTime() - utilDate) / 1000 * 60 * 60;

    public static String getTimeUntilMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

      return getDurationBreakdown(c.getTimeInMillis() - System.currentTimeMillis());
    }

    /**
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(hours);
        sb.append("h ");
        sb.append(minutes);
        sb.append("m ");

        return (sb.toString());
    }
}
