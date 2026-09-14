package br.com.pedrozulim.oab;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;

public final class Reminders {
    public static final String CHANNEL = "study_reminders";
    public static final String ACTION = "br.com.pedrozulim.oab.STUDY_REMINDER";
    public static final int COUNT = 3;
    private Reminders() {}
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("oab120", Context.MODE_PRIVATE); }
    public static int minutes(Context c, int slot) { return prefs(c).getInt("reminder.time." + slot, new int[]{480, 840, 1140}[slot]); }
    public static boolean enabled(Context c, int slot) { return prefs(c).getBoolean("reminder.on." + slot, false); }
    public static void channel(Context c) {
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Lembretes de estudo", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Horários escolhidos para estudar para a OAB");
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    public static boolean notificationsAllowed(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
        return manager.areNotificationsEnabled() && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }
    public static boolean exactAllowed(Context c) {
        return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    public static ZonedDateTime next(Context c, int slot) {
        if (!enabled(c, slot)) return null;
        SharedPreferences p = prefs(c);
        return ReminderTime.next(ZonedDateTime.now(), minutes(c, slot), LocalDate.parse(p.getString("start", LocalDate.now().toString())), p.getBoolean("extension", false) ? 126 : 120);
    }
    private static PendingIntent alarmIntent(Context c, int slot) {
        Intent intent = new Intent(c, ReminderReceiver.class).setAction(ACTION).putExtra("slot", slot);
        return PendingIntent.getBroadcast(c, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static void schedule(Context c) {
        channel(c);
        for (int slot = 0; slot < COUNT; slot++) scheduleSlot(c, slot);
    }
    public static void scheduleSlot(Context c, int slot) {
        if (slot < 0 || slot >= COUNT) return;
        AlarmManager manager = c.getSystemService(AlarmManager.class);
        PendingIntent intent = alarmIntent(c, slot);
        manager.cancel(intent);
        if (!enabled(c, slot)) c.getSystemService(NotificationManager.class).cancel(100 + slot);
        ZonedDateTime next = next(c, slot);
        if (next == null || !notificationsAllowed(c)) return;
        long when = next.toInstant().toEpochMilli();
        if (exactAllowed(c)) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, intent); return; }
            catch (SecurityException ignored) { /* Permission may change while scheduling. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, intent);
    }
}
