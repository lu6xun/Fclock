package com.example.fangj.fclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class TimerService extends Service {
    private static final String TAG = "TimerService";

    // 默认时间设置（分钟）
    private static final int DEFAULT_FOCUS_TIME = 25;
    private static final int DEFAULT_SHORT_BREAK = 5;
    private static final int DEFAULT_LONG_BREAK = 15;
    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;

    // 用户设置
    private int focusTimeMinutes = DEFAULT_FOCUS_TIME;
    private int shortBreakMinutes = DEFAULT_SHORT_BREAK;
    private int longBreakMinutes = DEFAULT_LONG_BREAK;
    private int sessionsBeforeLongBreak = SESSIONS_BEFORE_LONG_BREAK;

    // 计时器相关
    private CountDownTimer currentTimer;
    private TimerState timerState;
    private boolean isServiceStarted = false;

    // 广播接收器
    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_TIMER":
                        startTimer();
                        break;
                    case "PAUSE_TIMER":
                        pauseTimer();
                        break;
                    case "RESET_TIMER":
                        resetTimer();
                        break;
                    case "SKIP_PHASE":
                        skipToNextPhase();
                        break;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // 初始化计时器状态
        timerState = new TimerState();

        // 加载用户设置
        loadUserSettings();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("START_TIMER");
        filter.addAction("PAUSE_TIMER");
        filter.addAction("RESET_TIMER");
        filter.addAction("SKIP_PHASE");
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, filter);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (!isServiceStarted) {
            isServiceStarted = true;

            // 如果是从通知恢复，检查是否需要继续计时
            if (intent != null && intent.getBooleanExtra("RESTORE", false)) {
                if (timerState.getCurrentState() == TimerState.STATE_RUNNING) {
                    // 重新创建计时器（恢复运行）
                    long remaining = timerState.getRemainingTime();
                    if (remaining > 0) {
                        startNewTimer(remaining, timerState.getCurrentPhase());
                    }
                }
            }
        }

        // 显示通知
        showNotification();

        return START_STICKY; // 被系统杀死后尝试重新启动
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");

        // 取消广播接收器注册
        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver);

        // 停止计时器
        if (currentTimer != null) {
            currentTimer.cancel();
        }

        // 移除通知
        hideNotification();
    }

    // 创建通知渠道
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    TimerConstants.CHANNEL_ID,           // 渠道ID
                    TimerConstants.CHANNEL_NAME,         // 渠道名称
                    NotificationManager.IMPORTANCE_LOW   // 重要性级别
            );
            channel.setDescription("番茄时钟计时器通知");
            channel.setShowBadge(false);  // 不在应用图标上显示角标
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // 锁屏可见性

            // 可选：设置提示音、振动等
            channel.setSound(null, null);  // 无提示音
            channel.enableVibration(false); // 无振动

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "通知渠道创建成功: " + TimerConstants.CHANNEL_ID);
            }
        }
    }

    // 加载用户设置
    private void loadUserSettings() {
        SharedPreferences prefs = getSharedPreferences("FClockSettings", MODE_PRIVATE);
        focusTimeMinutes = prefs.getInt("focus_time", DEFAULT_FOCUS_TIME);
        shortBreakMinutes = prefs.getInt("short_break", DEFAULT_SHORT_BREAK);
        longBreakMinutes = prefs.getInt("long_break", DEFAULT_LONG_BREAK);
        sessionsBeforeLongBreak = prefs.getInt("sessions_before_long_break", SESSIONS_BEFORE_LONG_BREAK);
    }

    // 启动计时器
    private void startTimer() {
        if (timerState.getCurrentState() == TimerState.STATE_RUNNING) {
            return; // 已经在运行
        }

        if (timerState.getCurrentState() == TimerState.STATE_PAUSED) {
            // 从暂停状态恢复
            resumeTimer();
        } else {
            // 开始新的阶段
            int phase = timerState.getCurrentPhase();
            long duration = getPhaseDuration(phase);

            timerState.setTotalTime(duration);
            timerState.setRemainingTime(duration);
            timerState.setCurrentState(TimerState.STATE_RUNNING);

            startNewTimer(duration, phase);
        }

        updateNotification();
        broadcastTimerUpdate();
    }

    // 暂停计时器
    private void pauseTimer() {
        if (timerState.getCurrentState() != TimerState.STATE_RUNNING) {
            return;
        }

        if (currentTimer != null) {
            currentTimer.cancel();
        }

        timerState.setCurrentState(TimerState.STATE_PAUSED);
        updateNotification();
        broadcastTimerUpdate();
    }

    // 恢复计时器
    private void resumeTimer() {
        if (timerState.getCurrentState() != TimerState.STATE_PAUSED) {
            return;
        }

        long remaining = timerState.getRemainingTime();
        if (remaining > 0) {
            startNewTimer(remaining, timerState.getCurrentPhase());
            timerState.setCurrentState(TimerState.STATE_RUNNING);
            updateNotification();
            broadcastTimerUpdate();
        }
    }

    // 重置计时器
    private void resetTimer() {
        if (currentTimer != null) {
            currentTimer.cancel();
        }

        timerState.setCurrentState(TimerState.STATE_IDLE);
        timerState.setRemainingTime(0);

        updateNotification();
        broadcastTimerUpdate();
    }

    // 跳过当前阶段
    private void skipToNextPhase() {
        if (currentTimer != null) {
            currentTimer.cancel();
        }

        moveToNextPhase();
        broadcastPhaseChange();
    }

    // 移动到下一阶段
    private void moveToNextPhase() {
        int currentPhase = timerState.getCurrentPhase();

        if (currentPhase == TimerState.PHASE_FOCUS) {
            // 专注结束，完成一个番茄钟
            timerState.setCompletedSessions(timerState.getCompletedSessions() + 1);

            // 发送专注完成广播（用于弹出记录卡片）
            Intent completeIntent = new Intent(TimerConstants.ACTION_TIMER_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(completeIntent);

            // 判断是短休息还是长休息
            if (timerState.getCompletedSessions() % sessionsBeforeLongBreak == 0) {
                timerState.setCurrentPhase(TimerState.PHASE_LONG_BREAK);
            } else {
                timerState.setCurrentPhase(TimerState.PHASE_SHORT_BREAK);
            }
        } else {
            // 休息结束，回到专注
            timerState.setCurrentPhase(TimerState.PHASE_FOCUS);
        }

        // 开始新阶段的计时
        long duration = getPhaseDuration(timerState.getCurrentPhase());
        timerState.setTotalTime(duration);
        timerState.setRemainingTime(duration);
        timerState.setCurrentState(TimerState.STATE_RUNNING);

        startNewTimer(duration, timerState.getCurrentPhase());
    }

    // 启动新计时器
    private void startNewTimer(long durationMillis, final int phase) {
        if (currentTimer != null) {
            currentTimer.cancel();
        }

        currentTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerState.setRemainingTime(millisUntilFinished);
                broadcastTimerUpdate();
                updateNotification();
            }

            @Override
            public void onFinish() {
                timerState.setRemainingTime(0);
                timerState.setCurrentState(TimerState.STATE_IDLE);

                // 阶段完成，进入下一阶段
                moveToNextPhase();
                broadcastTimerUpdate();
                broadcastPhaseChange();
                updateNotification();
            }
        };

        currentTimer.start();
    }

    // 获取阶段持续时间（毫秒）
    private long getPhaseDuration(int phase) {
        int minutes;
        switch (phase) {
            case TimerState.PHASE_FOCUS:
                minutes = focusTimeMinutes;
                break;
            case TimerState.PHASE_SHORT_BREAK:
                minutes = shortBreakMinutes;
                break;
            case TimerState.PHASE_LONG_BREAK:
                minutes = longBreakMinutes;
                break;
            default:
                minutes = focusTimeMinutes;
        }
        return minutes * 60 * 1000L;
    }

    // 广播计时器更新
    private void broadcastTimerUpdate() {
        Intent intent = new Intent(TimerConstants.ACTION_TIMER_UPDATE);
        intent.putExtra(TimerConstants.EXTRA_TIMER_STATE, timerState.getCurrentState());
        intent.putExtra(TimerConstants.EXTRA_REMAINING_TIME, timerState.getRemainingTime());
        intent.putExtra(TimerConstants.EXTRA_PHASE_TYPE, timerState.getCurrentPhase());
        intent.putExtra(TimerConstants.EXTRA_COMPLETED_SESSIONS, timerState.getCompletedSessions());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // 广播阶段变更
    private void broadcastPhaseChange() {
        Intent intent = new Intent(TimerConstants.ACTION_PHASE_CHANGE);
        intent.putExtra(TimerConstants.EXTRA_PHASE_TYPE, timerState.getCurrentPhase());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // 显示通知
    private void showNotification() {
        Notification notification = createNotification();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(TimerConstants.NOTIFICATION_ID, notification);
        }
    }

    // 更新通知
    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(TimerConstants.NOTIFICATION_ID, notification);
        }
    }

    // 隐藏通知
    private void hideNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(TimerConstants.NOTIFICATION_ID);
        }
    }

    // 创建通知
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("RESTORE", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 创建操作按钮
        Intent startIntent = new Intent("START_TIMER");
        PendingIntent startPending = PendingIntent.getBroadcast(
                this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent pauseIntent = new Intent("PAUSE_TIMER");
        PendingIntent pausePending = PendingIntent.getBroadcast(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent resetIntent = new Intent("RESET_TIMER");
        PendingIntent resetPending = PendingIntent.getBroadcast(
                this, 2, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 重要：构建通知时必须指定渠道ID
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TimerConstants.CHANNEL_ID)
                .setContentTitle("番茄时钟")
                .setContentText(timerState.getPhaseName() + " - " + timerState.getFormattedTime())
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // 添加操作按钮
        if (timerState.getCurrentState() == TimerState.STATE_RUNNING) {
            builder.addAction(R.drawable.ic_pause, "暂停", pausePending);
        } else {
            builder.addAction(R.drawable.ic_play, "开始", startPending);
        }
        builder.addAction(R.drawable.ic_reset, "重置", resetPending);

        return builder.build();
    }
}