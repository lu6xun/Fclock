package com.example.fangj.fclock;

import java.io.Serializable;

public class TimerState implements Serializable {
    public static final int STATE_IDLE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_BREAK = 3;

    public static final int PHASE_FOCUS = 0;
    public static final int PHASE_SHORT_BREAK = 1;
    public static final int PHASE_LONG_BREAK = 2;

    private int currentState = STATE_IDLE;
    private int currentPhase = PHASE_FOCUS;
    private long remainingTime = 0;
    private long totalTime = 0;
    private int completedSessions = 0;

    public int getCurrentState() { return currentState; }
    public void setCurrentState(int currentState) { this.currentState = currentState; }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int currentPhase) { this.currentPhase = currentPhase; }

    public long getRemainingTime() { return remainingTime; }
    public void setRemainingTime(long remainingTime) { this.remainingTime = remainingTime; }

    public long getTotalTime() { return totalTime; }
    public void setTotalTime(long totalTime) { this.totalTime = totalTime; }

    public int getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

    public String getPhaseName() {
        switch (currentPhase) {
            case PHASE_FOCUS: return "专注中";
            case PHASE_SHORT_BREAK: return "短休息";
            case PHASE_LONG_BREAK: return "长休息";
            default: return "准备";
        }
    }

    public String getFormattedTime() {
        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}