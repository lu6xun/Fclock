package com.example.fangj.fclock;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Delete;
import java.util.List;

@Dao
public interface FocusSessionDao {

    @Insert
    void insert(FocusSession session);

    // 异步查询（返回LiveData）
    @Query("SELECT * FROM focus_sessions ORDER BY date DESC")
    LiveData<List<FocusSession>> getAllSessions();

    // 同步查询（直接返回List）
    @Query("SELECT * FROM focus_sessions ORDER BY date DESC")
    List<FocusSession> getAllSessionsSync();

    // 新增：删除所有数据
    @Query("DELETE FROM focus_sessions")
    void deleteAll();
}