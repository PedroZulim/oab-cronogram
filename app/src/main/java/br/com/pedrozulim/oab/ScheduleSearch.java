package br.com.pedrozulim.oab;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared search rules, independent of the Android UI. */
final class ScheduleSearch {
    private static final Pattern DAY = Pattern.compile("(?:dia\\s*)?0*([0-9]+)");

    static boolean matches(int day, String title, String summary, String query) {
        String normalized = normalize(query);
        Matcher dayQuery = DAY.matcher(normalized);
        if (dayQuery.matches()) {
            // Compare strings so arbitrarily large input cannot overflow an integer.
            return String.valueOf(day).equals(dayQuery.group(1));
        }
        return normalize(title).contains(normalized)
                || normalize(summary).contains(normalized);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
