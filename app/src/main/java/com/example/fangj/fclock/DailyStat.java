package com.example.fangj.fclock;

public class DailyStat {
    private String date;
    private int totalMinutes;

    public DailyStat(String date, int totalMinutes) {
        this.date = date;
        this.totalMinutes = totalMinutes;
    }

    public String getDate() { return date; }
    public int getTotalMinutes() { return totalMinutes; }

    public void addDuration(int minutes) {
        this.totalMinutes += minutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }
}