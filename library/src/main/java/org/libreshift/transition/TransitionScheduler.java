package org.libreshift.transition;

import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public abstract class TransitionScheduler extends BroadcastReceiver {

    static final String TAG = "TransitionScheduler";
    static final String ACTION_ALARM = "org.libreshift.transition.ACTION_ALARM";
    static final String EXTRA_START = "org.libreshift.transition.EXTRA_START";

    abstract ValueAnimator newAnimation(String id);

    // Is UPDATE_CURRENT the right flag? Red Moon was using the constant '0',
    // which doesn't match the constant of *any* of the PendingIntent flags..
    static final int FLAG = PendingIntent.FLAG_UPDATE_CURRENT;

    // TODO: Store more than one transition at once.
    Transition transition = null;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_ALARM:
                    String id = intent.getDataString();
                    SharedPreferences prefs = getPrefs(context);
                    long start = prefs.getLong(EXTRA_START, -1);
                    if (start == -1) {
                        ValueAnimator animator = newAnimation(id);
                        transition = new Transition(context, animator, start);
                        transition.start();
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    reschedule(context);
                    break;
            }
        }
    }

    // Passing an id of an alarm that is already scheduled will overwrite that alarm
    public void schedule(Context context, String id, long startAtMillis) {
        // TODO: If there's already an alarm scheduled, should we return the old time?

        // TODO: If startAtMillis is in the past, start the transition right away
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putLong(id, startAtMillis).apply();

        PendingIntent pi = getIntent(context, id);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, startAtMillis, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, startAtMillis, pi);
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
        prefs.edit().remove(id).apply();

        if (transition != null) {
            transition.cancel();
            transition = null;
        }
    }

    void reschedule(Context context) {
        SharedPreferences prefs = getPrefs(context);
        for (String id : prefs.getAll().keySet()) {
            Long startTime = prefs.getLong(id, -1);
            if (startTime != -1) {
                schedule(context, id, startTime);
            } else {
                Log.e(TAG, "Couldn't get start time for alarm: " + id);
            }
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
