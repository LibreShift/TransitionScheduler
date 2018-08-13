package com.libreshift.transition;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

public class Transition implements BroadcastReceiver, Animator.AnimatorListener {

    static final String TAG = "Transition";

    private Context context;
    private long startTime;
    private ValueAnimator animator;

    Transition(Context ctx, ValueAnimator animation, long startAtMillis) {
        context = ctx;
        startTime = startAtMillis;
        animator = animation;
        animator.addListener(this);
    }

    public void start() {
        long duration = animator.getDuration();
        long delay = System.currentTimeMillis() - startTime;
        animator.setCurrentPlayTime(delay);
        if (animator.isPaused()) {
            animator.resume();
        } else if (!animator.isRunning()) {
            animator.start();
        } else {
            Log.w(TAG, "Tried to start animation that was already running!");
        }
    }

    public void cancel() {
        animator.cancel();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_SCREEN_ON:
                Log.d(TAG, "Screen turned on");
                start();
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.d(TAG, "Screen turned off");
                if (animator.isRunning()) {
                    animator.pause();
                } else {
                    Log.w(TAG, "Tried to pause animation that was not yet running!");
                }
                break;
        }
    }

    public void onAnimationStart(Animator animation) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(this, filter);
    }

    public void onAnimationEnd(Animator animation) {
        context.unregisterReceiver(this);
    }

    public void onAnimationCancel(Animator animation) {}

    public void onAnimationRepeat(Animator animation) {}
}
