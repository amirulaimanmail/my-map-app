package com.example.mymapapp.utils;

import java.util.Locale;

public class UtilUnitConversion {
    public static String formatSecondsToTime(double seconds) {
        if (seconds < 60) {
            return "1 min"; // No decimals for seconds
        } else if (seconds < 3600) {
            int minutes = (int) (seconds / 60); // Use int for minutes
            return String.format(Locale.US, "%d min", minutes);
        } else {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            if (minutes > 0) {
                return String.format(Locale.US, "%d h %d min", hours, minutes);
            } else {
                return String.format(Locale.US, "%d h", hours);
            }
        }
    }

    public static String formatMetersToKilometers(double meters) {
        if (meters < 1000) {
            return (int) meters + " m";
        } else {
            double kilometers = meters / 1000.0;
            return String.format(Locale.US, "%.1f km", kilometers);
        }
    }
}
