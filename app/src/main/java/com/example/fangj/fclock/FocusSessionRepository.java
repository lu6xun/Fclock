package com.example.fangj.fclock;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.util.Log;
import java.util.List;

public class FocusSessionRepository {
    private static final String TAG = "FocusSessionRepository";
    private FocusSessionDao dao;
    private LiveData<List<FocusSession>> allSessions;

    public FocusSessionRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        dao = database.focusSessionDao();
        allSessions = dao.getAllSessions();
        Log.d(TAG, "Repository initialized");
    }

    // 插入数据（异步）
    public void insert(FocusSession session) {
        Log.d(TAG, "Inserting session: " + session.getTaskName());
        new InsertAsyncTask(dao).execute(session);
    }

    // 获取所有数据（异步，LiveData）
    public LiveData<List<FocusSession>> getAllSessions() {
        Log.d(TAG, "Getting all sessions (LiveData)");
        return allSessions;
    }

    // 同步获取所有数据
    public List<FocusSession> getAllSessionsSync() {
        try {
            Log.d(TAG, "Getting all sessions (sync)");
            return dao.getAllSessionsSync();
        } catch (Exception e) {
            Log.e(TAG, "Error getting sessions: " + e.getMessage());
            return null;
        }
    }

    // 新增：清空所有数据
    public void clearAllData() {
        new ClearAllAsyncTask(dao).execute();
    }

    // 获取今天日期字符串的静态方法
    public static String getTodayDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // 异步插入任务
    private static class InsertAsyncTask extends AsyncTask<FocusSession, Void, Void> {
        private FocusSessionDao dao;

        InsertAsyncTask(FocusSessionDao dao) {
            this.dao = dao;
        }

        @Override
        protected Void doInBackground(FocusSession... sessions) {
            dao.insert(sessions[0]);
            Log.d(TAG, "Session inserted: " + sessions[0].getTaskName());
            return null;
        }
    }

    // 新增：异步清空数据任务
    private static class ClearAllAsyncTask extends AsyncTask<Void, Void, Void> {
        private FocusSessionDao dao;

        ClearAllAsyncTask(FocusSessionDao dao) {
            this.dao = dao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dao.deleteAll();
            Log.d(TAG, "All data cleared");
            return null;
        }
    }
}