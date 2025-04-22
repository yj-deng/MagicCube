package com.example.magiccube.utils;

import android.os.Handler;
import android.os.Looper;

public class MyTimer {
    private long startTime = 0L;
    private long pausedTime = 0L;
    private boolean isRunning = false;
    private final Handler handler;
    private OnTimeUpdateListener timeListener;
    private OnStateChangeListener stateListener;

    public interface OnTimeUpdateListener {
        void onTimeUpdate(String formattedTime);
    }

    public interface OnStateChangeListener {
        void onStateChange(boolean isRunning, long pausedTime);
    }

    public MyTimer() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void setOnTimeUpdateListener(OnTimeUpdateListener listener) {
        this.timeListener = listener;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.stateListener = listener;
    }

    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis() - pausedTime;
            isRunning = true;
            notifyStateChange();
            handler.post(updateRunnable);
        }
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            pausedTime = System.currentTimeMillis() - startTime;
            notifyStateChange();
            handler.removeCallbacks(updateRunnable);
        }
    }

    public void reset() {
        isRunning = false;
        startTime = System.currentTimeMillis();
        pausedTime = 0L;
        handler.removeCallbacks(updateRunnable);
        notifyStateChange();
        updateTime();
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                updateTime();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTime() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        String time = String.format("%02d:%02d", minutes, seconds);
        if (timeListener != null) {
            timeListener.onTimeUpdate(time);
        }
    }

    private void notifyStateChange() {
        if (stateListener != null) {
            stateListener.onStateChange(isRunning, pausedTime);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
    public long getElapsedMillis() {
        if (isRunning) {
            return System.currentTimeMillis() - startTime;
        } else {
            return pausedTime;
        }
    }
}