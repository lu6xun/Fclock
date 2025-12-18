package com.example.fangj.fclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

// 新增集合类导入
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Glide 导入
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

public class MainActivity extends AppCompatActivity
        implements RecordSessionDialogFragment.RecordSessionDialogListener { // 实现对话框监听器
    private static final String TAG = "MainActivity";

    // 计时器UI组件
    private TextView timerText;
    private TextView phaseText;
    private TextView sessionsText;
    private Button startButton;
    private Button pauseButton;
    private Button resetButton;
    private Button skipButton;
    private Button btnStats;

    // GIF 背景
    private ImageView gifBackground;

    // 音频UI组件
    private RecyclerView soundRecyclerView;
    private SeekBar volumeSeekBar;
    private Button toggleAudioButton;
    private TextView currentSoundText;

    // 新增适配器变量
    private AmbientSoundAdapter soundAdapter;

    // 状态和数据
    private TimerState currentState = new TimerState();
    private AudioPlayerManager audioManager;
    private FocusSessionRepository mRepository;

    // 广播接收器
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case TimerConstants.ACTION_TIMER_UPDATE:
                        updateTimerUI(intent);
                        break;
                    case TimerConstants.ACTION_TIMER_COMPLETE:
                        showRecordDialog();
                        break;
                    case TimerConstants.ACTION_PHASE_CHANGE:
                        updatePhaseUI(intent);
                        break;
                }
            }
        }
    };

    // 音频状态监听器
    private AudioPlayerManager.OnPlaybackStateChangeListener audioListener =
            new AudioPlayerManager.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(final boolean isPlaying) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleAudioButton.setText(isPlaying ? "暂停环境音" : "播放环境音");
                        }
                    });
                }

                @Override
                public void onSoundChanged(final AmbientSound sound) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentSoundText.setText("当前: " + sound.getName());
                            // 更新 RecyclerView 选中状态
                            if (soundAdapter != null) {
                                soundAdapter.setSelectedSoundId(sound.getId());
                            }
                        }
                    });
                }

                @Override
                public void onVolumeChanged(final float volume) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            volumeSeekBar.setProgress((int)(volume * 100));
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initAudio(); // 此方法将调用新的 initRecyclerView

        // 初始化数据仓库
        mRepository = new FocusSessionRepository(getApplication());
        Log.d(TAG, "FocusSessionRepository 初始化完成");

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerConstants.ACTION_TIMER_UPDATE);
        filter.addAction(TimerConstants.ACTION_TIMER_COMPLETE);
        filter.addAction(TimerConstants.ACTION_PHASE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, filter);

        // 启动服务
        Intent serviceIntent = new Intent(this, TimerService.class);
        if (getIntent().getBooleanExtra("RESTORE", false)) {
            serviceIntent.putExtra("RESTORE", true);
        }
        startService(serviceIntent);

        // 启动音频服务
        startService(new Intent(this, AudioService.class));

        // 加载 GIF 背景
        loadGifBackground();

        // 测试数据库连接（可选）
        testDatabaseConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerReceiver);

        // 移除音频监听器
        if (audioManager != null) {
            audioManager.removePlaybackStateChangeListener(audioListener);
        }

        // 清理 Glide 资源
        if (gifBackground != null) {
            Glide.with(this).clear(gifBackground);
        }
    }

    private void initViews() {
        // GIF 背景
        gifBackground = findViewById(R.id.gif_background);

        // 统计按钮
        btnStats = findViewById(R.id.btn_stats);
        btnStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });

        // 调试按钮（长按统计按钮触发调试）
        btnStats.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                checkSavedData();
                return true;
            }
        });

        // 计时器相关视图
        timerText = findViewById(R.id.timer_text);
        phaseText = findViewById(R.id.phase_text);
        sessionsText = findViewById(R.id.sessions_text);
        startButton = findViewById(R.id.start_button);
        pauseButton = findViewById(R.id.pause_button);
        resetButton = findViewById(R.id.reset_button);
        skipButton = findViewById(R.id.skip_button);

        // 音频相关视图
        soundRecyclerView = findViewById(R.id.sound_recycler_view);
        volumeSeekBar = findViewById(R.id.volume_seekbar);
        toggleAudioButton = findViewById(R.id.toggle_audio_button);
        currentSoundText = findViewById(R.id.current_sound_text);

        // 设置计时器按钮监听器
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService("START_TIMER");
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService("PAUSE_TIMER");
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService("RESET_TIMER");
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService("SKIP_PHASE");
            }
        });

        // 初始化UI
        updateUI();
    }

    // 加载 GIF 背景
    private void loadGifBackground() {
        try {
            // 从 assets 文件夹加载 GIF
            Glide.with(this)
                    .asGif()
                    .load("file:///android_asset/background.gif")
                    .apply(new RequestOptions()
                            .centerCrop()
                            .placeholder(android.R.color.white)
                            .error(android.R.color.white))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(gifBackground);

            Log.d(TAG, "GIF 背景加载成功");
        } catch (Exception e) {
            Log.e(TAG, "GIF 背景加载失败: " + e.getMessage());
            // 设置默认白色背景
            gifBackground.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    private void initAudio() {
        // 初始化音频管理器
        audioManager = AudioPlayerManager.getInstance(this);

        // 设置音量控制
        volumeSeekBar.setProgress((int)(audioManager.getVolume() * 100));
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100.0f;
                    audioManager.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 设置音频播放/暂停按钮
        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioManager.togglePlayPause();
            }
        });

        // 初始化 RecyclerView
        initRecyclerView();

        // 注册音频状态监听
        audioManager.addPlaybackStateChangeListener(audioListener);

        // 更新当前音效显示
        AmbientSound currentSound = audioManager.getCurrentSound();
        if (currentSound != null) {
            currentSoundText.setText("当前: " + currentSound.getName());
            // 同时设置适配器的选中状态
            if (soundAdapter != null) {
                soundAdapter.setSelectedSoundId(currentSound.getId());
            }
        }
    }

    private void initRecyclerView() {
        // 1. 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        soundRecyclerView.setLayoutManager(layoutManager);

        // 2. 准备数据
        final List<AmbientSound> soundList = new ArrayList<>();
        Collections.addAll(soundList, AmbientSound.getPresetSounds());

        // 3. 创建并设置适配器
        soundAdapter = new AmbientSoundAdapter(soundList, new AmbientSoundAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                AmbientSound selectedSound = soundList.get(position);
                audioManager.playSound(selectedSound);
            }
        });
        soundRecyclerView.setAdapter(soundAdapter);
    }

    private void sendActionToService(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateTimerUI(Intent intent) {
        int state = intent.getIntExtra(TimerConstants.EXTRA_TIMER_STATE, TimerState.STATE_IDLE);
        long remainingTime = intent.getLongExtra(TimerConstants.EXTRA_REMAINING_TIME, 0);
        int phase = intent.getIntExtra(TimerConstants.EXTRA_PHASE_TYPE, TimerState.PHASE_FOCUS);
        int sessions = intent.getIntExtra(TimerConstants.EXTRA_COMPLETED_SESSIONS, 0);

        currentState.setCurrentState(state);
        currentState.setRemainingTime(remainingTime);
        currentState.setCurrentPhase(phase);
        currentState.setCompletedSessions(sessions);

        updateUI();
    }

    private void updatePhaseUI(Intent intent) {
        int phase = intent.getIntExtra(TimerConstants.EXTRA_PHASE_TYPE, TimerState.PHASE_FOCUS);
        currentState.setCurrentPhase(phase);

        // 更新阶段文本
        phaseText.setText(currentState.getPhaseName());

        // 如果进入休息阶段，可以暂停音频
        if (phase != TimerState.PHASE_FOCUS) {
            if (audioManager != null) {
                audioManager.pause();
            }
        }
    }

    private void updateUI() {
        // 更新计时器文本
        timerText.setText(currentState.getFormattedTime());

        // 更新阶段文本
        phaseText.setText(currentState.getPhaseName());

        // 更新完成的番茄钟数
        sessionsText.setText("已完成: " + currentState.getCompletedSessions() + " 个番茄钟");

        // 更新按钮状态
        boolean isRunning = currentState.getCurrentState() == TimerState.STATE_RUNNING;
        boolean isPaused = currentState.getCurrentState() == TimerState.STATE_PAUSED;

        startButton.setEnabled(!isRunning);
        pauseButton.setEnabled(isRunning);
        resetButton.setEnabled(isRunning || isPaused);
        skipButton.setEnabled(isRunning || isPaused);

        // 根据状态更新按钮文字颜色（可选）
        updateButtonColors();
    }

    // 更新按钮颜色（可选功能）
    private void updateButtonColors() {
        // 这里可以根据当前状态调整按钮颜色
        // 例如：专注阶段用绿色，休息阶段用蓝色
        int currentPhase = currentState.getCurrentPhase();

        if (currentPhase == TimerState.PHASE_FOCUS) {
            // 专注阶段
            startButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            phaseText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (currentPhase == TimerState.PHASE_SHORT_BREAK) {
            // 短休息阶段
            startButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            phaseText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else if (currentPhase == TimerState.PHASE_LONG_BREAK) {
            // 长休息阶段
            startButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            phaseText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
    }

    // ============ 实现记录对话框逻辑 ============
    private void showRecordDialog() {
        Log.d(TAG, "专注完成，显示记录对话框");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 获取当前专注时长
                int focusDuration = 25;

                // 获取当前播放的环境音
                String ambientSound = "无";
                if (audioManager != null && audioManager.getCurrentSound() != null) {
                    ambientSound = audioManager.getCurrentSound().getName();
                }

                // 创建并显示对话框
                FragmentManager fm = getSupportFragmentManager();
                RecordSessionDialogFragment dialog = RecordSessionDialogFragment.newInstance(
                        focusDuration, ambientSound);
                dialog.show(fm, "record_dialog");
            }
        });
    }

    // ============ 实现记录对话框监听器 ============
    @Override
    public void onRecordSaved(FocusSession session) {
        // 保存记录到数据库
        if (mRepository != null) {
            mRepository.insert(session);
            Log.d(TAG, "专注记录已保存到数据库: " + session.getTaskName() +
                    " | 日期: " + session.getDate() +
                    " | 时长: " + session.getActualDuration() + "分钟");

            // 立即检查数据是否保存成功
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500); // 等待500ms确保数据保存
                        checkSavedData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Log.e(TAG, "Repository 为空，无法保存记录");
        }

        // 显示保存成功的提示
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "记录已保存", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRecordCancelled() {
        // 用户选择忽略记录
        Log.d(TAG, "用户忽略了本次专注记录");
        Toast.makeText(this, "已跳过记录", Toast.LENGTH_SHORT).show();
    }

    // ============ 新增调试方法 ============

    // 检查保存的数据
    private void checkSavedData() {
        if (mRepository != null) {
            // 在新线程中查询数据
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final List<FocusSession> sessions = mRepository.getAllSessionsSync();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (sessions != null && !sessions.isEmpty()) {
                                    StringBuilder message = new StringBuilder();

                                    message.append("数据库中有 ").append(sessions.size()).append(" 条记录\n");

                                    // 只显示最近3条记录
                                    int count = Math.min(sessions.size(), 3);
                                    for (int i = 0; i < count; i++) {
                                        FocusSession session = sessions.get(i);
                                        message.append(i + 1).append(". ")
                                                .append(session.getTaskName())
                                                .append(" (").append(session.getActualDuration()).append("分钟)\n");
                                    }

                                    if (sessions.size() > 3) {
                                        message.append("... 还有 ").append(sessions.size() - 3).append(" 条记录");
                                    }

                                    Toast.makeText(MainActivity.this, message.toString(), Toast.LENGTH_LONG).show();

                                    // 同时在Logcat中打印完整信息
                                    for (FocusSession session : sessions) {
                                        Log.d("DB_DEBUG", "记录: " + session.getTaskName() +
                                                " | ID: " + session.getId() +
                                                " | 日期: " + session.getDate() +
                                                " | 时长: " + session.getActualDuration() + "分钟" +
                                                " | 环境音: " + session.getAmbientSound() +
                                                " | 标签: " + session.getTags());
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            "数据库中暂无记录",
                                            Toast.LENGTH_LONG).show();
                                    Log.d("DB_DEBUG", "数据库中暂无记录");
                                }
                            }
                        });
                    } catch (final Exception e) {
                        final String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
                        Log.e("DB_DEBUG", "查询失败: " + errorMessage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "查询失败: " + errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        } else {
            Toast.makeText(this, "Repository 未初始化", Toast.LENGTH_SHORT).show();
            Log.e("DB_DEBUG", "Repository 为空");
        }
    }

    // 测试数据库连接
    private void testDatabaseConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 尝试访问数据库
                    AppDatabase db = AppDatabase.getInstance(getApplication());
                    if (db != null) {
                        Log.d(TAG, "数据库连接测试: 成功");

                        // 检查表是否存在
                        FocusSessionDao dao = db.focusSessionDao();
                        if (dao != null) {
                            Log.d(TAG, "DAO 初始化: 成功");

                            // 尝试查询记录数量
                            List<FocusSession> sessions = dao.getAllSessionsSync();
                            Log.d(TAG, "当前记录数: " + (sessions != null ? sessions.size() : 0));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "数据库连接测试失败: " + e.getMessage());
                }
            }
        }).start();
    }
}