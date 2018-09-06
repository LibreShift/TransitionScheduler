package org.libreshift.transition;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public abstract class TransitionScheduler extends BroadcastReceiver {

    static final String TAG = "TransitionScheduler";
    static final String ACTION_ALARM = "org.libreshift.transition.ACTION_ALARM";
    static final String IDS = "IDS";
    static final String START = "_START";
    static final String DURATION = "_DURATION";

    // One hour; override for a different default
    static long DURATION_DEFAULT = 3600000;

    // It is important to compare the start time to the current time,
    // since alarms may be delayed
    abstract void onAlarm(String id, long startTime, long duration);

    // Is UPDATE_CURRENT the right flag? Red Moon was using the constant '0',
    // which doesn't match the constant of *any* of the PendingIntent flags..
    static final int FLAG = PendingIntent.FLAG_UPDATE_CURRENT;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_ALARM:
                    String id = intent.getDataString();
                    String id_start = id + START;
                    String id_duration = id + DURATION;
                    SharedPreferences prefs = getPrefs(context);
                    long start = prefs.getLong(id_start, System.currentTimeMillis());
                    long duration = prefs.getLong(id_duration, DURATION_DEFAULT);
                    onAlarm(id, start, duration);
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    reschedule(context);
                    break;
            }
        }
    }

    // Passing an id of an alarm that is already scheduled will overwrite that alarm
    public void schedule(Context context, String id, long startTime, long duration) {
        // TODO: If there's already an alarm scheduled, should we return the old time?

        long now = System.currentTimeMillis();
        long endTime = startTime + duration;
        if (now >= endTime) {
            return; // TODO: Should we do anything here?
        }
        SharedPreferences prefs = getPrefs(context);
        Set<String> ids = new HashSet<>(prefs.getStringSet(IDS, new HashSet<String>()));
        String id_start = id + START;
        String id_duration = id + DURATION;
        ids.add(id);
        prefs.edit()
            .putStringSet(IDS, ids)
            .putLong(id_start, startTime)
            .putLong(id_duration, duration)
            .apply();

        PendingIntent pi = getIntent(context, id);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, startTime, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, startTime, pi);
            }
        }
    }

    public void cancel(Context context, String id) {
        PendingIntent pi = getIntent(context, id);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
        }

        SharedPreferences prefs = getPrefs(context);
        Set<String> ids = new HashSet<>(prefs.getStringSet(IDS, new HashSet<String>()));
        ids.remove(id);
        String id_start = id + START;
        String id_duration = id + DURATION;
        prefs.edit().putStringSet(IDS, ids).remove(id_start).remove(id_duration).apply();
    }

    void reschedule(Context context) {
        SharedPreferences prefs = getPrefs(context);
        Set<String> ids = prefs.getStringSet(IDS, new HashSet<String>());

        for (String id : ids) {
            String id_start = id + START;
            String id_duration = id + DURATION;
            long startTime = prefs.getLong(id_start, 0);
            long duration = prefs.getLong(id_duration, DURATION_DEFAULT);
            cancel(context, id);
            schedule(context, id, startTime, duration);
        }
    }

    PendingIntent getIntent(Context context, String id) {
        Uri data = Uri.parse(id);
        Intent intent = new Intent(ACTION_ALARM, data, context, this.getClass());
        return PendingIntent.getBroadcast(context, 0, intent, FLAG);
    }

    static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
    }

    static final String PREFERENCE = "org.libreshift.transition.preference";
}
