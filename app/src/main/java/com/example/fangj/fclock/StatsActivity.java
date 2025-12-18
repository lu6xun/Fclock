package com.example.fangj.fclock;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private TextView textTodayTotal;
    private TextView textWeekTotal;
    private TextView textStreakDays;

    private LineChart chartDailyDuration;
    private PieChart chartEfficiencyPie;
    private BarChart chartSoundPreference;

    private StatsViewModel viewModel;

    // 新增按钮变量
    private Button btnClearData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        initViews();
        setupViewModel();
        setupCharts();

        // 添加刷新按钮
        addRefreshButton();
    }

    private void initViews() {
        textTodayTotal = findViewById(R.id.text_today_total);
        textWeekTotal = findViewById(R.id.text_week_total);
        textStreakDays = findViewById(R.id.text_streak_days);

        chartDailyDuration = findViewById(R.id.chart_daily_duration);
        chartEfficiencyPie = findViewById(R.id.chart_efficiency_pie);
        chartSoundPreference = findViewById(R.id.chart_sound_preference);

        // 获取按钮
        btnClearData = findViewById(R.id.btn_clear_data);
        Button btnBack = findViewById(R.id.btn_back);

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 设置清空数据按钮点击事件
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataConfirmation();
            }
        });
    }

    // 显示清空数据确认对话框
    private void showClearDataConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认清空数据");
        builder.setMessage("确定要清空所有专注记录数据吗？\n\n此操作不可撤销！");

        builder.setPositiveButton("确定清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAllDataSimple();  // 使用简化版本
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置确定按钮为红色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    // 简化的清空数据方法（不依赖ViewModel刷新）
    private void clearAllDataSimple() {
        // 第一步：立即更新UI（不用等待数据库操作）
        textTodayTotal.setText("0 分钟");
        textWeekTotal.setText("0 分钟");
        textStreakDays.setText("0 天");

        // 清空图表显示
        clearAllCharts();

        // 第二步：在后台线程清空数据库（不影响UI）
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 清空数据库中的所有数据
                    AppDatabase db = AppDatabase.getInstance(getApplication());
                    db.focusSessionDao().deleteAll();

                    // 记录日志
                    Log.d("StatsActivity", "数据库数据已清空");

                } catch (Exception e) {
                    Log.e("StatsActivity", "清空数据库失败: " + e.getMessage());
                    // 数据库操作失败不影响UI
                }
            }
        }).start();

        // 显示成功提示
        Toast.makeText(this, "数据已清空", Toast.LENGTH_SHORT).show();
    }

    // 清空所有图表
    private void clearAllCharts() {
        // 清空折线图
        chartDailyDuration.clear();
        chartDailyDuration.setNoDataText("暂无近7日数据");
        chartDailyDuration.setNoDataTextColor(Color.GRAY);
        chartDailyDuration.invalidate();

        // 清空饼图
        chartEfficiencyPie.clear();
        chartEfficiencyPie.setCenterText("暂无数据");
        chartEfficiencyPie.invalidate();

        // 清空柱状图
        chartSoundPreference.clear();
        chartSoundPreference.setNoDataText("暂无环境音数据");
        chartSoundPreference.setNoDataTextColor(Color.GRAY);
        chartSoundPreference.invalidate();
    }

    private void setupViewModel() {
        viewModel = ViewModelProviders.of(this).get(StatsViewModel.class);

        // 观察数据变化
        viewModel.getTodayTotal().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer totalMinutes) {
                if (totalMinutes != null) {
                    textTodayTotal.setText(totalMinutes + " 分钟");
                }
            }
        });

        viewModel.getWeekTotal().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer totalMinutes) {
                if (totalMinutes != null) {
                    textWeekTotal.setText(totalMinutes + " 分钟");
                }
            }
        });

        viewModel.getStreakDays().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer days) {
                if (days != null) {
                    textStreakDays.setText(days + " 天");
                }
            }
        });

        viewModel.getWeeklyData().observe(this, new Observer<List<DailyStat>>() {
            @Override
            public void onChanged(List<DailyStat> dailyStats) {
                updateLineChart(dailyStats);
            }
        });

        viewModel.getEfficiencyData().observe(this, new Observer<Map<String, Integer>>() {
            @Override
            public void onChanged(Map<String, Integer> efficiencyMap) {
                updatePieChart(efficiencyMap);
            }
        });

        viewModel.getSoundPreferenceData().observe(this, new Observer<Map<String, Integer>>() {
            @Override
            public void onChanged(Map<String, Integer> soundMap) {
                updateBarChart(soundMap);
            }
        });
    }

    private void addRefreshButton() {
        Button btnRefresh = new Button(this);
        btnRefresh.setText("刷新数据");
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewModel != null) {
                    viewModel.refreshData();
                    Toast.makeText(StatsActivity.this, "正在刷新数据...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 添加到标题栏旁边
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(btnRefresh);
            actionBar.setDisplayShowCustomEnabled(true);
        }
    }

    private void setupCharts() {
        // 设置折线图基本样式
        setupLineChart();

        // 设置环形图基本样式
        setupPieChart();

        // 设置柱状图基本样式
        setupBarChart();
    }

    private void setupLineChart() {
        chartDailyDuration.getDescription().setEnabled(false);
        chartDailyDuration.setTouchEnabled(true);
        chartDailyDuration.setDragEnabled(true);
        chartDailyDuration.setScaleEnabled(true);
        chartDailyDuration.setDrawGridBackground(false);
        chartDailyDuration.setPinchZoom(true);

        XAxis xAxis = chartDailyDuration.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chartDailyDuration.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        chartDailyDuration.getAxisRight().setEnabled(false);
        chartDailyDuration.getLegend().setEnabled(false);
    }

    private void setupPieChart() {
        chartEfficiencyPie.getDescription().setEnabled(false);
        chartEfficiencyPie.setExtraOffsets(5, 10, 5, 5);
        chartEfficiencyPie.setDragDecelerationFrictionCoef(0.95f);
        chartEfficiencyPie.setDrawHoleEnabled(true);
        chartEfficiencyPie.setHoleColor(Color.WHITE);
        chartEfficiencyPie.setTransparentCircleColor(Color.WHITE);
        chartEfficiencyPie.setTransparentCircleAlpha(110);
        chartEfficiencyPie.setHoleRadius(58f);
        chartEfficiencyPie.setTransparentCircleRadius(61f);
        chartEfficiencyPie.setDrawCenterText(true);
        chartEfficiencyPie.setRotationAngle(0);
        chartEfficiencyPie.setRotationEnabled(true);
        chartEfficiencyPie.setHighlightPerTapEnabled(true);

        chartEfficiencyPie.getLegend().setEnabled(true);
        chartEfficiencyPie.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chartEfficiencyPie.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chartEfficiencyPie.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chartEfficiencyPie.getLegend().setDrawInside(false);
        chartEfficiencyPie.getLegend().setXEntrySpace(7f);
        chartEfficiencyPie.getLegend().setYEntrySpace(0f);
        chartEfficiencyPie.getLegend().setYOffset(0f);
    }

    private void setupBarChart() {
        chartSoundPreference.getDescription().setEnabled(false);
        chartSoundPreference.setDrawGridBackground(false);
        chartSoundPreference.setDrawBarShadow(false);
        chartSoundPreference.setDrawValueAboveBar(true);
        chartSoundPreference.setMaxVisibleValueCount(60);
        chartSoundPreference.setPinchZoom(false);
        chartSoundPreference.setDrawGridBackground(false);

        XAxis xAxis = chartSoundPreference.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(4);

        YAxis leftAxis = chartSoundPreference.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setSpaceTop(15f);

        chartSoundPreference.getAxisRight().setEnabled(false);
        chartSoundPreference.getLegend().setEnabled(false);
    }

    private void updateLineChart(List<DailyStat> dailyStats) {
        try {
            if (dailyStats == null || dailyStats.isEmpty()) {
                // 没有数据时显示提示
                chartDailyDuration.clear();
                chartDailyDuration.setNoDataText("暂无近7日数据");
                chartDailyDuration.setNoDataTextColor(Color.GRAY);
                return;
            }

            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

            for (int i = 0; i < dailyStats.size(); i++) {
                DailyStat stat = dailyStats.get(i);
                entries.add(new Entry(i, stat.getTotalMinutes()));

                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.getDate());
                    labels.add(sdf.format(date));
                } catch (Exception e) {
                    labels.add(stat.getDate().substring(5)); // 显示 "MM-DD"
                }
            }

            LineDataSet dataSet = new LineDataSet(entries, "专注时长");
            dataSet.setColor(Color.parseColor("#2196F3"));
            dataSet.setCircleColor(Color.parseColor("#2196F3"));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(false);
            dataSet.setValueTextSize(10f);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#E3F2FD"));
            dataSet.setFillAlpha(100);

            LineData lineData = new LineData(dataSet);
            chartDailyDuration.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartDailyDuration.setData(lineData);
            chartDailyDuration.invalidate();
        } catch (Exception e) {
            Log.e("StatsActivity", "更新折线图失败: " + e.getMessage());
            chartDailyDuration.clear();
            chartDailyDuration.setNoDataText("数据加载失败");
            chartDailyDuration.setNoDataTextColor(Color.RED);
        }
    }

    private void updatePieChart(Map<String, Integer> efficiencyMap) {
        try {
            if (efficiencyMap == null || efficiencyMap.isEmpty()) {
                chartEfficiencyPie.clear();
                chartEfficiencyPie.setCenterText("暂无数据");
                return;
            }

            List<PieEntry> entries = new ArrayList<>();
            int total = 0;

            for (Map.Entry<String, Integer> entry : efficiencyMap.entrySet()) {
                if (entry.getValue() > 0) {
                    entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    total += entry.getValue();
                }
            }

            if (entries.isEmpty()) {
                chartEfficiencyPie.clear();
                chartEfficiencyPie.setCenterText("暂无标签数据");
                return;
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            // 使用预定义的颜色模板
            ArrayList<Integer> colors = new ArrayList<>();
            for (int c : ColorTemplate.VORDIPLOM_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.JOYFUL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.COLORFUL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.LIBERTY_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.PASTEL_COLORS)
                colors.add(c);

            dataSet.setColors(colors);

            PieData data = new PieData(dataSet);
            data.setValueTextSize(11f);
            data.setValueTextColor(Color.WHITE);

            chartEfficiencyPie.setCenterText("总记录\n" + total + "次");
            chartEfficiencyPie.setCenterTextSize(14f);
            chartEfficiencyPie.setData(data);
            chartEfficiencyPie.invalidate();
        } catch (Exception e) {
            Log.e("StatsActivity", "更新饼图失败: " + e.getMessage());
            chartEfficiencyPie.clear();
            chartEfficiencyPie.setCenterText("加载失败");
        }
    }

    private void updateBarChart(Map<String, Integer> soundMap) {
        try {
            if (soundMap == null || soundMap.isEmpty()) {
                chartSoundPreference.clear();
                chartSoundPreference.setNoDataText("暂无环境音数据");
                chartSoundPreference.setNoDataTextColor(Color.GRAY);
                return;
            }

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int index = 0;
            for (Map.Entry<String, Integer> entry : soundMap.entrySet()) {
                entries.add(new BarEntry(index, entry.getValue()));
                labels.add(entry.getKey());
                index++;
            }

            BarDataSet dataSet = new BarDataSet(entries, "平均专注时长");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(10f);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.4f);

            chartSoundPreference.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartSoundPreference.setData(barData);
            chartSoundPreference.invalidate();
        } catch (Exception e) {
            Log.e("StatsActivity", "更新柱状图失败: " + e.getMessage());
            chartSoundPreference.clear();
            chartSoundPreference.setNoDataText("加载失败");
            chartSoundPreference.setNoDataTextColor(Color.RED);
        }
    }
}