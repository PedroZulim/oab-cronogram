package br.com.pedrozulim.oab;

import android.app.*;
import android.content.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Reminders.ACTION.equals(intent.getAction())) {
            int slot = intent.getIntExtra("slot", -1);
            SharedPreferences prefs = Reminders.prefs(context);
            int day = (int) ChronoUnit.DAYS.between(LocalDate.parse(prefs.getString("start", LocalDate.now().toString())), LocalDate.now()) + 1;
            int limit = prefs.getBoolean("extension", false) ? 126 : 120;
            if (slot >= 0 && slot < Reminders.COUNT && Reminders.enabled(context, slot)
                    && Reminders.notificationsAllowed(context) && day >= 1 && day <= limit
                    && !prefs.getBoolean("day." + day + ".done", false)) {
                Reminders.channel(context);
                Intent open = new Intent(context, MainActivity.class).setAction("open_study").putExtra("study_day", day)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent content = PendingIntent.getActivity(context, 100 + slot, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Notification notification = new Notification.Builder(context, Reminders.CHANNEL)
                        .setSmallIcon(br.com.pedrozulim.oab.R.drawable.ic_launcher_monochrome)
                        .setContentTitle("Hora de estudar · Dia " + day)
                        .setContentText("Seu próximo passo na OAB: abra o estudo de hoje.")
                        .setContentIntent(content).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build();
                try { context.getSystemService(NotificationManager.class).notify(100 + slot, notification); }
                catch (SecurityException ignored) { /* Notifications were disabled in settings. */ }
            }
            Reminders.scheduleSlot(context, slot);
        } else {
            Reminders.schedule(context);
        }
    }
}
