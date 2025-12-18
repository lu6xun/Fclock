package com.example.fangj.fclock;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import java.util.Date;
import java.util.List;

// 定义表名
@Entity(tableName = "focus_sessions")
// 使用类型转换器来处理复杂类型（如List<String>和Date）
@TypeConverters({Converters.class})
public class FocusSession {

    // 自增主键
    @PrimaryKey(autoGenerate = true)
    private long id;

    // 开始时间戳
    @ColumnInfo(name = "start_time")
    private long startTime;

    // 计划专注时长（分钟）
    @ColumnInfo(name = "planned_duration")
    private int plannedDuration;

    // 实际专注时长（分钟）
    @ColumnInfo(name = "actual_duration")
    private int actualDuration;

    // 任务描述（可选）
    @ColumnInfo(name = "task_name")
    private String taskName;

    // 状态标签列表，如 ["高效", "平稳"]
    private List<String> tags;

    // 使用的环境音场景名
    @ColumnInfo(name = "ambient_sound")
    private String ambientSound;

    // 记录的日期（格式：yyyy-MM-dd），便于按日查询
    private String date;

    // 构造方法
    public FocusSession(long startTime, int plannedDuration, int actualDuration,
                        String taskName, List<String> tags, String ambientSound, String date) {
        this.startTime = startTime;
        this.plannedDuration = plannedDuration;
        this.actualDuration = actualDuration;
        this.taskName = taskName;
        this.tags = tags;
        this.ambientSound = ambientSound;
        this.date = date;
    }

    // Getter 和 Setter 方法 (Room 需要)
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public int getPlannedDuration() { return plannedDuration; }
    public void setPlannedDuration(int plannedDuration) { this.plannedDuration = plannedDuration; }

    public int getActualDuration() { return actualDuration; }
    public void setActualDuration(int actualDuration) { this.actualDuration = actualDuration; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getAmbientSound() { return ambientSound; }
    public void setAmbientSound(String ambientSound) { this.ambientSound = ambientSound; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}