package com.example.fangj.fclock;

public class AmbientSound {
    private int id;
    private String name;
    private String description;
    private int iconResId;
    private int audioResId;

    public static final int SOUND_LIBRARY_RAIN = 0;
    public static final int SOUND_STUDY_NIGHT = 1;
    public static final int SOUND_FOREST_STREAM = 2;
    public static final int SOUND_CAMPFIRE = 3;

    public AmbientSound(int id, String name, String description, int iconResId, int audioResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResId = iconResId;
        this.audioResId = audioResId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public int getAudioResId() { return audioResId; }

    public static AmbientSound[] getPresetSounds() {
        return new AmbientSound[] {
                new AmbientSound(SOUND_LIBRARY_RAIN, "图书馆细雨", "雨声与海浪声",
                        R.drawable.ic_sound_rain, R.raw.sound_sea_rain),
                new AmbientSound(SOUND_STUDY_NIGHT, "深夜书房", "翻书声和雨声",
                        R.drawable.ic_sound_study, R.raw.sound_study_night),
                new AmbientSound(SOUND_FOREST_STREAM, "林间溪流", "流水声与鸟鸣",
                        R.drawable.ic_sound_forest, R.raw.sound_forest_stream),
                new AmbientSound(SOUND_CAMPFIRE, "篝火营地", "柴火噼啪声",
                        R.drawable.ic_sound_campfire, R.raw.sound_campfire)
        };
    }
}