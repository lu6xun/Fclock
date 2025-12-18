package com.example.fangj.fclock;

public class TimerConstants {
    public static final String ACTION_TIMER_UPDATE = "com.example.fangj.fclock.TIMER_UPDATE";
    public static final String ACTION_TIMER_COMPLETE = "com.example.fangj.fclock.TIMER_COMPLETE";
    public static final String ACTION_PHASE_CHANGE = "com.example.fangj.fclock.PHASE_CHANGE";

    public static final String EXTRA_TIMER_STATE = "timer_state";
    public static final String EXTRA_REMAINING_TIME = "remaining_time";
    public static final String EXTRA_PHASE_TYPE = "phase_type";
    public static final String EXTRA_COMPLETED_SESSIONS = "completed_sessions";

    public static final int NOTIFICATION_ID = 1001;
    public static final String CHANNEL_ID = "fclock_timer_channel";
    public static final String CHANNEL_NAME = "番茄时钟计时器";
}