package com.libreshift;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

public class TransitionScheduler extends BroadcastReceiver {

    static final String ACTION_ALARM = "com.libreshift.timer.ACTION_ALARM";
    static final String TAG = "com.libreshift.timer.TransitionScheduler";

    long startAt;
    ValueAnimator animator;

    // Is UPDATE_CURRENT the right flag? Red Moon was using the constant '0',
    // which doesn't match the contstant of *any* of the PendingIntent flags..
    static final int FLAG = PendingIntent.FLAG_UPDATE_CURRENT;
    
    // Runs the animation at the given time, *only* when the screen is on
    // Returns an id for the job
    public int schedule(Context context, ValueAnimator animation, long startAtMillis) {
        // TODO: Persist these values so you can *actually* use this to schedule
        startAt = startAtMillis;
        animator = animation;

        int id = newUniqueId();
        PendingIntent pi = getIntent(context, id);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            // What is the right AlarmManager callback to use here?
            // How is setExactAndAllowWhileIdle different from setExact with RTC_WAKEUP?
            // Is RTC_WAKEUP necessary or would AlarmManger.RTC be okay?
            am.setExact(AlarmManager.RTC_WAKEUP, startAtMillis, pi);
        }
        return id;
    }

    public void cancel(Context context, int id) {
        if (id == 0) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                PendingIntent pi = getIntent(context, id);
                am.cancel(pi);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // TODO: Reschedule alarms on boot
        long delay;
        switch (action) {
            case ACTION_ALARM:
                int id = Integer.parseInt(intent.getData().toString());
                animator.addListener(new TransitionListener(context, this, id));
                delay = System.currentTimeMillis() - startAt;
                animator.setCurrentPlayTime(delay);
                animator.start();
                break;
            case Intent.ACTION_SCREEN_ON:
                Log.d(TAG, "Screen turned on");
                delay = System.currentTimeMillis() - startAt;
                animator.setCurrentPlayTime(delay);
                if (animator.isPaused()) {
                    animator.resume();
                }
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.d(TAG, "Screen turned off");
                if (animator.isRunning()) {
                    animator.pause();
                }
                break;
        }
    }

    int newUniqueId() {
        // TODO: generate unique id
        return 0;
    }

    PendingIntent getIntent(Context context, int id) {
        // We need the id in the data field so each is unique, for AlarmManager
        Uri data = Uri.parse(Integer.toString(id));
        Intent intent = new Intent(ACTION_ALARM, data, context, this.getClass());
        return PendingIntent.getBroadcast(context, id, intent, FLAG);
    }

    private class TransitionListener implements Animator.AnimatorListener {
        int id;
        Context context;
        BroadcastReceiver receiver;

        TransitionListener(Context ctx, BroadcastReceiver rcv, int n) {
            id = n;
            context = ctx;
            receiver = rcv;
        }

        public void onAnimationStart(Animator animation) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            context.registerReceiver(receiver, filter);
        }
        public void onAnimationEnd(Animator animation) {
            context.unregisterReceiver(receiver);
        }
        public void onAnimationCancel(Animator animation) {}
        public void onAnimationRepeat(Animator animation) {}
        // public void onAnimationUpdate(ValueAnimator animation) {}
    }
}
