package com.huanchengfly.tieba.post.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.huanchengfly.tieba.post.R;
import com.huanchengfly.tieba.post.components.MyViewHolder;
import com.huanchengfly.tieba.post.interfaces.OnItemClickListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TranslucentThemeColorAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private static final int[] sColors = new int[]{
            Color.parseColor("#FF4477E0"),
            Color.parseColor("#FFFF9A9E"),
            Color.parseColor("#FFC51100"),
            Color.parseColor("#FF000000"),
            Color.parseColor("#FF512DA8")
    };
    private final WeakReference<Context> mContextWeakReference;
    private final List<Integer> mColors = new ArrayList<>();
    private OnItemClickListener<Integer> mOnItemClickListener;
    private int mSelectedColor;

    public TranslucentThemeColorAdapter(Context context) {
        mContextWeakReference = new WeakReference<>(context);
    }

    public OnItemClickListener<Integer> getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener<Integer> onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return mContextWeakReference.get();
    }

    public void setSelectedColor(int color) {
        mSelectedColor = color;
        notifyDataSetChanged();
    }

    public void setPalette(Palette palette) {
        mColors.clear();
        int[] colors = new int[]{
                palette.getVibrantColor(Color.TRANSPARENT),
                palette.getMutedColor(Color.TRANSPARENT),
                palette.getDominantColor(Color.TRANSPARENT)
        };
        for (int color : colors) {
            if (color != Color.TRANSPARENT) mColors.add(color);
        }
        for (int color : sColors) {
            mColors.add(color);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(getContext(), R.layout.item_theme_color);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setItemOnClickListener(v -> {
            mSelectedColor = mColors.get(position);
            notifyDataSetChanged();
            if (getOnItemClickListener() != null) {
                getOnItemClickListener().onClick(holder.itemView, mColors.get(position), position, 0);
            }
        });
        MaterialCardView preview = holder.getView(R.id.theme_preview);
        preview.setCardBackgroundColor(mColors.get(position));
        holder.itemView.setSelected(mSelectedColor == mColors.get(position));
        holder.itemView.setContentDescription(String.format(java.util.Locale.ROOT, "#%08X", mColors.get(position)));
        if (mSelectedColor == mColors.get(position)) {
            holder.setVisibility(R.id.theme_selected, View.VISIBLE);
        } else {
            holder.setVisibility(R.id.theme_selected, View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mColors.size();
    }
}
