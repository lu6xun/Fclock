package com.example.fangj.fclock;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerManager {
    private static final String TAG = "AudioPlayerManager";
    private static AudioPlayerManager instance;

    private Context context;
    private MediaPlayer mediaPlayer;
    private AmbientSound currentSound;
    private float volume = 0.5f;
    private boolean isPlaying = false;

    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSoundChanged(AmbientSound sound);
        void onVolumeChanged(float volume);
    }

    private List<OnPlaybackStateChangeListener> listeners = new ArrayList<>();

    private AudioPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        initMediaPlayer();
    }

    public static synchronized AudioPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioPlayerManager(context);
        }
        return instance;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "播放完成");
                isPlaying = false;
                notifyPlaybackStateChanged(false);
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "播放错误: what=" + what + ", extra=" + extra);
                isPlaying = false;
                releaseMediaPlayer();
                initMediaPlayer();
                return true;
            }
        });
    }

    public void playSound(AmbientSound sound) {
        if (sound == null) {
            Log.e(TAG, "音效为空");
            return;
        }

        if (currentSound != null && currentSound.getId() == sound.getId() && isPlaying) {
            return;
        }

        stop();

        currentSound = sound;

        try {
            mediaPlayer.reset();

            // 使用兼容的方式加载资源
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(sound.getAudioResId());
            if (afd != null) {
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } else {
                Log.e(TAG, "无法打开音频资源: " + sound.getName());
                return;
            }

            mediaPlayer.prepare();
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.start();
            isPlaying = true;
            notifySoundChanged(sound);
            notifyPlaybackStateChanged(true);
            Log.d(TAG, "开始播放: " + sound.getName());

        } catch (IOException e) {
            Log.e(TAG, "播放音效失败: " + sound.getName(), e);
            releaseMediaPlayer();
            initMediaPlayer();
        } catch (IllegalStateException e) {
            Log.e(TAG, "播放器状态异常", e);
            releaseMediaPlayer();
            initMediaPlayer();
        } catch (Exception e) {
            Log.e(TAG, "未知错误", e);
            releaseMediaPlayer();
            initMediaPlayer();
        }
    }

    public void togglePlayPause() {
        if (currentSound == null) {
            Log.w(TAG, "没有选择音效");
            return;
        }

        if (isPlaying) {
            pause();
        } else {
            playCurrentSound();
        }
    }

    public void playCurrentSound() {
        if (currentSound != null) {
            playSound(currentSound);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            notifyPlaybackStateChanged(false);
            Log.d(TAG, "暂停播放");
        }
    }

    public void resume() {
        if (mediaPlayer != null && currentSound != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            notifyPlaybackStateChanged(true);
            Log.d(TAG, "恢复播放");
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            isPlaying = false;
            notifyPlaybackStateChanged(false);
            Log.d(TAG, "停止播放");
        }
    }

    public void setVolume(float volume) {
        if (volume < 0) volume = 0;
        if (volume > 1) volume = 1;

        this.volume = volume;

        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.setVolume(volume, volume);
        }

        notifyVolumeChanged(volume);
        Log.d(TAG, "音量设置为: " + volume);
    }

    public float getVolume() {
        return volume;
    }

    public AmbientSound getCurrentSound() {
        return currentSound;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void release() {
        stop();
        releaseMediaPlayer();
        listeners.clear();
        instance = null;
        Log.d(TAG, "释放音频播放器");
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void addPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removePlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStateChanged(final boolean isPlaying) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (OnPlaybackStateChangeListener listener : listeners) {
                    listener.onPlaybackStateChanged(isPlaying);
                }
            }
        });
    }

    private void notifySoundChanged(final AmbientSound sound) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (OnPlaybackStateChangeListener listener : listeners) {
                    listener.onSoundChanged(sound);
                }
            }
        });
    }

    private void notifyVolumeChanged(final float volume) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (OnPlaybackStateChangeListener listener : listeners) {
                    listener.onVolumeChanged(volume);
                }
            }
        });
    }
}