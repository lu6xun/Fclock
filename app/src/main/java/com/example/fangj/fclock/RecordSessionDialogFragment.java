package com.example.fangj.fclock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class RecordSessionDialogFragment extends DialogFragment {

    public interface RecordSessionDialogListener {
        void onRecordSaved(FocusSession session);
        void onRecordCancelled();
    }

    private RecordSessionDialogListener mListener;
    private int mPlannedDuration;
    private String mAmbientSound;

    public static RecordSessionDialogFragment newInstance(int plannedDuration, String ambientSound) {
        RecordSessionDialogFragment fragment = new RecordSessionDialogFragment();
        Bundle args = new Bundle();
        args.putInt("plannedDuration", plannedDuration);
        args.putString("ambientSound", ambientSound);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (RecordSessionDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " 必须实现 RecordSessionDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mPlannedDuration = args.getInt("plannedDuration", 25);
            mAmbientSound = args.getString("ambientSound", "无");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_record_session, null);

        // 初始化视图组件
        initViews(dialogView);

        builder.setView(dialogView)
                .setCancelable(false);

        return builder.create();
    }

    private void initViews(final View view) {
        // 获取所有需要的视图组件并创建final副本
        final EditText editTaskName = view.findViewById(R.id.edit_task_name);

        final CheckBox cbEfficient = view.findViewById(R.id.cb_efficient);
        final CheckBox cbSteady = view.findViewById(R.id.cb_steady);
        final CheckBox cbDistracted = view.findViewById(R.id.cb_distracted);
        final CheckBox cbInterrupted = view.findViewById(R.id.cb_interrupted);

        // 环境音显示
        TextView textAmbientSound = view.findViewById(R.id.text_ambient_sound);
        textAmbientSound.setText(mAmbientSound);

        // 专注时长显示
        TextView textSessionDuration = view.findViewById(R.id.text_session_duration);
        textSessionDuration.setText(mPlannedDuration + "分钟");

        // 保存按钮点击事件
        view.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = editTaskName.getText().toString().trim();
                if (taskName.isEmpty()) {
                    taskName = "未命名任务";
                }

                // 收集选中的标签
                List<String> selectedTags = new ArrayList<>();
                if (cbEfficient.isChecked()) selectedTags.add("高效");
                if (cbSteady.isChecked()) selectedTags.add("平稳");
                if (cbDistracted.isChecked()) selectedTags.add("轻微走神");
                if (cbInterrupted.isChecked()) selectedTags.add("频繁中断");

                // 如果用户什么都没选，添加一个默认标签
                if (selectedTags.isEmpty()) {
                    selectedTags.add("未记录");
                }

                // 创建专注记录对象
                FocusSession session = new FocusSession(
                        System.currentTimeMillis() - (mPlannedDuration * 60 * 1000L),
                        mPlannedDuration,
                        mPlannedDuration,
                        taskName,
                        selectedTags,
                        mAmbientSound,
                        FocusSessionRepository.getTodayDateString()
                );

                // 通知监听器
                if (mListener != null) {
                    mListener.onRecordSaved(session);
                }

                // 关闭对话框
                dismiss();
            }
        });

        // 取消按钮点击事件
        view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRecordCancelled();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mListener != null) {
            mListener.onRecordCancelled();
        }
    }
}