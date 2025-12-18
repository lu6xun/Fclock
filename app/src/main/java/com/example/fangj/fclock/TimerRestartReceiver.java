package com.example.fangj.fclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class TimerRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerRestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "收到广播: " + action);

        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED)
                || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {

            SharedPreferences prefs = context.getSharedPreferences("TimerState", Context.MODE_PRIVATE);
            boolean isRunning = prefs.getBoolean("is_running", false);
            long endTime = prefs.getLong("end_time", 0);

            if (isRunning && endTime > System.currentTimeMillis()) {
                Intent serviceIntent = new Intent(context, TimerService.class);
                serviceIntent.putExtra("RESTORE", true);
                context.startService(serviceIntent);
                Log.d(TAG, "重新启动计时器服务");
            }
        }
    }
}