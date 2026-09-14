package br.com.pedrozulim.oab;

import java.time.*;

public class ReminderTimeTest {
    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 9, 14);
        check("2026-09-14T08:00-03:00[America/Sao_Paulo]", "2026-09-14T07:59-03:00[America/Sao_Paulo]", 480, start, 120);
        check("2026-09-15T08:00-03:00[America/Sao_Paulo]", "2026-09-14T08:00-03:00[America/Sao_Paulo]", 480, start, 120);
        check("2026-09-15T00:00-03:00[America/Sao_Paulo]", "2026-09-14T23:59-03:00[America/Sao_Paulo]", 0, start, 120);
        check("2026-09-14T08:00-03:00[America/Sao_Paulo]", "2026-09-10T10:00-03:00[America/Sao_Paulo]", 480, start, 120);
        check(null, "2026-09-14T09:00-03:00[America/Sao_Paulo]", 480, start, 1);
        check(null, "2027-09-14T09:00-03:00[America/Sao_Paulo]", 480, start, 126);
        // Spring DST gap resolves to the next valid local time; next day returns to 02:30.
        check("2026-03-08T03:30-04:00[America/New_York]", "2026-03-07T23:00-05:00[America/New_York]", 150, LocalDate.of(2026,3,1), 120);
        check("2026-03-09T02:30-04:00[America/New_York]", "2026-03-08T03:30-04:00[America/New_York]", 150, LocalDate.of(2026,3,1), 120);
        check("2026-11-02T01:30-05:00[America/New_York]", "2026-11-01T01:30-04:00[America/New_York]", 90, LocalDate.of(2026,10,1), 120);
        System.out.println("ReminderTime: all checks passed.");
    }
    private static void check(String expected, String now, int minutes, LocalDate start, int days) {
        ZonedDateTime actual = ReminderTime.next(ZonedDateTime.parse(now), minutes, start, days);
        if (expected == null ? actual != null : !ZonedDateTime.parse(expected).equals(actual))
            throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
