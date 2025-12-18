package com.example.fangj.fclock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class AudioService extends Service {
    private static final String TAG = "AudioService";

    private AudioPlayerManager audioManager;
    private BroadcastReceiver audioControlReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AudioService onCreate");

        audioManager = AudioPlayerManager.getInstance(this);

        audioControlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case "PLAY_SOUND":
                            int soundId = intent.getIntExtra("sound_id", -1);
                            if (soundId >= 0) {
                                AmbientSound[] sounds = AmbientSound.getPresetSounds();
                                if (soundId < sounds.length) {
                                    audioManager.playSound(sounds[soundId]);
                                }
                            }
                            break;

                        case "TOGGLE_PLAY_PAUSE":
                            audioManager.togglePlayPause();
                            break;

                        case "SET_VOLUME":
                            float volume = intent.getFloatExtra("volume", 0.5f);
                            audioManager.setVolume(volume);
                            break;

                        case "STOP_AUDIO":
                            audioManager.stop();
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAY_SOUND");
        filter.addAction("TOGGLE_PLAY_PAUSE");
        filter.addAction("SET_VOLUME");
        filter.addAction("STOP_AUDIO");
        LocalBroadcastManager.getInstance(this).registerReceiver(audioControlReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AudioService onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AudioService onDestroy");

        if (audioControlReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(audioControlReceiver);
        }
    }
}