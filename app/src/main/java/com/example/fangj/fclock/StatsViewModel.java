package com.example.fangj.fclock;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsViewModel extends AndroidViewModel {

    private FocusSessionRepository repository;

    private MutableLiveData<Integer> todayTotal = new MutableLiveData<>();
    private MutableLiveData<Integer> weekTotal = new MutableLiveData<>();
    private MutableLiveData<Integer> streakDays = new MutableLiveData<>();
    private MutableLiveData<List<DailyStat>> weeklyData = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> efficiencyData = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> soundPreferenceData = new MutableLiveData<>();

    // 添加一个标志，避免重复计算
    private boolean isCalculating = false;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        repository = new FocusSessionRepository(application);

        // 初始化默认值
        setDefaultValues();

        // 开始加载数据
        loadAllData();
    }

    private void setDefaultValues() {
        todayTotal.setValue(0);
        weekTotal.setValue(0);
        streakDays.setValue(0);
        weeklyData.setValue(new ArrayList<DailyStat>());
        efficiencyData.setValue(new HashMap<String, Integer>());
        soundPreferenceData.setValue(new HashMap<String, Integer>());
    }

    private void loadAllData() {
        if (isCalculating) return;
        isCalculating = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 关键修改：不使用 LiveData 的 getValue()，而是直接查询数据库
                    List<FocusSession> allSessions = repository.getAllSessionsSync();

                    if (allSessions == null || allSessions.isEmpty()) {
                        setDefaultValues();
                        return;
                    }

                    // 计算所有统计数据
                    calculateTodayTotal(allSessions);
                    calculateWeekTotal(allSessions);
                    calculateStreakDays(allSessions);
                    calculateWeeklyData(allSessions);
                    calculateEfficiencyData(allSessions);
                    calculateSoundPreferenceData(allSessions);

                } catch (Exception e) {
                    e.printStackTrace();
                    setDefaultValues();
                } finally {
                    isCalculating = false;
                }
            }
        }).start();
    }

    private void calculateTodayTotal(List<FocusSession> sessions) {
        String today = getTodayDateString();
        int total = 0;

        for (FocusSession session : sessions) {
            if (today.equals(session.getDate())) {
                total += session.getActualDuration();
            }
        }

        todayTotal.postValue(total);
    }

    private void calculateWeekTotal(List<FocusSession> sessions) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = getDateStringDaysAgo(7);

        int total = 0;
        for (FocusSession session : sessions) {
            if (session.getDate().compareTo(startDate) >= 0) {
                total += session.getActualDuration();
            }
        }

        weekTotal.postValue(total);
    }

    private void calculateStreakDays(List<FocusSession> sessions) {
        // 简化的连续打卡计算：只要今天有记录就是1天
        String today = getTodayDateString();
        int streak = 0;

        for (FocusSession session : sessions) {
            if (today.equals(session.getDate())) {
                streak = 1;
                break;
            }
        }

        streakDays.postValue(streak);
    }

    private void calculateWeeklyData(List<FocusSession> sessions) {
        List<DailyStat> weeklyStats = new ArrayList<>();

        // 获取最近7天的日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String dateStr = sdf.format(calendar.getTime());

            DailyStat stat = new DailyStat(dateStr, 0);

            // 计算当天的总时长
            for (FocusSession session : sessions) {
                if (dateStr.equals(session.getDate())) {
                    stat.addDuration(session.getActualDuration());
                }
            }

            weeklyStats.add(stat);
        }

        weeklyData.postValue(weeklyStats);
    }

    private void calculateEfficiencyData(List<FocusSession> sessions) {


        Map<String, Integer> efficiencyMap = new HashMap<>();

        // 初始化所有可能的标签
        String[] allTags = {"高效", "平稳", "轻微走神", "频繁中断", "疲惫", "未记录"};
        for (String tag : allTags) {
            efficiencyMap.put(tag, 0);
        }

        // 统计每个标签的出现次数
        for (FocusSession session : sessions) {
            List<String> tags = session.getTags();
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    if (efficiencyMap.containsKey(tag)) {
                        efficiencyMap.put(tag, efficiencyMap.get(tag) + 1);
                    }
                }
            }
        }

        efficiencyData.postValue(efficiencyMap);
    }

    private void calculateSoundPreferenceData(List<FocusSession> sessions) {
        Map<String, Integer> soundMap = new HashMap<>();
        Map<String, Integer> soundCountMap = new HashMap<>();

        // 初始化所有环境音
        AmbientSound[] allSounds = AmbientSound.getPresetSounds();
        for (AmbientSound sound : allSounds) {
            soundMap.put(sound.getName(), 0);
            soundCountMap.put(sound.getName(), 0);
        }

        // 统计每个环境音的总时长和使用次数
        for (FocusSession session : sessions) {
            String soundName = session.getAmbientSound();
            if (soundMap.containsKey(soundName)) {
                int currentTotal = soundMap.get(soundName);
                int currentCount = soundCountMap.get(soundName);

                soundMap.put(soundName, currentTotal + session.getActualDuration());
                soundCountMap.put(soundName, currentCount + 1);
            }
        }

        // 计算平均时长
        Map<String, Integer> resultMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : soundMap.entrySet()) {
            String soundName = entry.getKey();
            int totalDuration = entry.getValue();
            int count = soundCountMap.get(soundName);

            if (count > 0) {
                resultMap.put(soundName, totalDuration / count);
            } else {
                resultMap.put(soundName, 0);
            }
        }

        soundPreferenceData.postValue(resultMap);
    }

    // 辅助方法：获取今天日期字符串
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // 辅助方法：获取几天前的日期字符串
    private String getDateStringDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    // Getter 方法
    public LiveData<Integer> getTodayTotal() { return todayTotal; }
    public LiveData<Integer> getWeekTotal() { return weekTotal; }
    public LiveData<Integer> getStreakDays() { return streakDays; }
    public LiveData<List<DailyStat>> getWeeklyData() { return weeklyData; }
    public LiveData<Map<String, Integer>> getEfficiencyData() { return efficiencyData; }
    public LiveData<Map<String, Integer>> getSoundPreferenceData() { return soundPreferenceData; }

    // 刷新数据的方法
    public void refreshData() {
        loadAllData();
    }
}