package com.ashok.simplereader.utils;

/**
 * Created by ashok on 22/4/17.
 */

public class DateTimeUtil {
    public static String convert(long millis) {
        long difference = (System.currentTimeMillis() / 1000) - (millis / 1000);

        long minutes = difference / 60;
        if (minutes < 60) {
            return (int) minutes + "m";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return (int) hours + "h";
        }

        long days = hours / 24;
        if (days < 30) {
            return (int) days + "d";
        }

        long months = days / 30;
        if (months < 12) {
            return (int) months + "mo";
        }

        return (int) months / 12 + "y";

    }
}
