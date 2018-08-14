package org.libreshift.transition;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;

public class Transition extends BroadcastReceiver implements Animator.AnimatorListener {

    private static final String TAG = "Transition";

    private Context context;
    private long startTime;
    private ValueAnimator animator;
    private ArrayList<Animator.AnimatorListener> listeners = null;

    Transition(Context ctx, ValueAnimator animation, long startAtMillis) {
        context = ctx;
        startTime = startAtMillis;
        animator = animation;
        animator.addListener(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    Log.d(TAG, "Screen turned on");
                    resume();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    Log.d(TAG, "Screen turned off");
                    pause();
                    break;
            }
        }
    }

    public void start() {
        long delay = System.currentTimeMillis() - startTime;
        animator.setCurrentPlayTime(delay);
        if (!animator.isRunning()) {
            animator.start();
        } else {
            Log.w(TAG, "Tried to start animation that was already running!");
        }
    }

    private void pause() {
        listeners = new ArrayList<>(animator.getListeners());
        animator.removeAllListeners();
        if (animator.isRunning()) {
            animator.cancel();
        } else {
            Log.w(TAG, "Tried to pause animation that was not yet running!");
        }
    }

    private void resume() {
        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                Animator.AnimatorListener l = listeners.get(i);
                if (l != null) {
                    animator.addListener(l);
                }
            }
            listeners = null;
        } else {
            Log.w(TAG, "No listeners to restore!");
        }
        start();
    }

    public void cancel() {
        resume();
        animator.cancel();
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
