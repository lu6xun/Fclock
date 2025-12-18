package com.example.fangj.fclock;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class AmbientSoundAdapter extends RecyclerView.Adapter<AmbientSoundAdapter.ViewHolder> {

    private List<AmbientSound> mSoundList;
    private OnItemClickListener mListener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public AmbientSoundAdapter(List<AmbientSound> soundList, OnItemClickListener listener) {
        mSoundList = soundList;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ambient_sound, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AmbientSound sound = mSoundList.get(position);

        // 创建final副本供内部类使用
        final int finalPosition = position;

        holder.soundIcon.setImageResource(sound.getIconResId());
        holder.soundName.setText(sound.getName());
        holder.soundDescription.setText(sound.getDescription());

        boolean isSelected = (position == selectedPosition);
        holder.playingIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // 设置点击监听器
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    int previousSelected = selectedPosition;
                    selectedPosition = finalPosition;
                    notifyItemChanged(previousSelected);
                    notifyItemChanged(selectedPosition);
                    mListener.onItemClick(finalPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSoundList.size();
    }

    public void setSelectedSoundId(int soundId) {
        for (int i = 0; i < mSoundList.size(); i++) {
            if (mSoundList.get(i).getId() == soundId) {
                int oldPosition = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 明确使用完整类名
        public android.support.v7.widget.CardView cardView;
        ImageView soundIcon;
        TextView soundName;
        TextView soundDescription;
        ImageView playingIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            // 关键：添加类型转换
            cardView = (android.support.v7.widget.CardView) itemView.findViewById(R.id.card_view);
            soundIcon = itemView.findViewById(R.id.sound_icon);
            soundName = itemView.findViewById(R.id.sound_name);
            soundDescription = itemView.findViewById(R.id.sound_description);
            playingIndicator = itemView.findViewById(R.id.playing_indicator);
        }
    }
}