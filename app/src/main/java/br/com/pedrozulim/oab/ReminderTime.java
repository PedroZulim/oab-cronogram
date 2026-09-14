package br.com.pedrozulim.oab;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Calendar-based recurrence preserves local time across time zone/DST changes. */
public final class ReminderTime {
    private ReminderTime() {}
    public static ZonedDateTime next(ZonedDateTime now, int minutes, LocalDate start, int days) {
        if (minutes < 0 || minutes >= 1440 || days < 1) throw new IllegalArgumentException();
        LocalDate date = now.toLocalDate().isBefore(start) ? start : now.toLocalDate();
        ZonedDateTime next = date.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(now.getZone());
        if (!next.isAfter(now)) next = date.plusDays(1).atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(now.getZone());
        return next.toLocalDate().isBefore(start.plusDays(days)) ? next : null;
    }
}
