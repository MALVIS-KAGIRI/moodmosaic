package com.moodmosaic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.moodmosaic.databinding.ItemDayBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RecyclerView adapter that turns calendar dates into colored grid cells.
public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.DayViewHolder> {

    // Callback used when the user taps a day cell.
    public interface OnDayClickListener {
        void onDayClick(String date);
    }

    private static final String EMPTY = "";

    private final List<String> visibleDates;
    private final OnDayClickListener onDayClickListener;
    private final Map<String, String> moodMap = new HashMap<>();

    // Receives the visible calendar cells and the click behavior from the screen.
    public MoodAdapter(List<String> visibleDates, OnDayClickListener onDayClickListener) {
        this.visibleDates = visibleDates;
        this.onDayClickListener = onDayClickListener;
    }

    // Replaces the in-memory mood lookup map and refreshes the grid.
    public void submitMoods(List<DayMood> moods) {
        moodMap.clear();
        for (DayMood mood : moods) {
            moodMap.put(mood.getDate(), mood.getColorHex());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // Inflates one day tile view for the grid.
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDayBinding binding = ItemDayBinding.inflate(inflater, parent, false);
        return new DayViewHolder(binding);
    }

    @Override
    // Binds either an empty spacer cell or a real day with its saved color.
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String date = visibleDates.get(position);
        if (EMPTY.equals(date)) {
            holder.bindEmpty();
            return;
        }

        String[] parts = date.split("-");
        String dayValue = parts[2];
        String moodColor = moodMap.get(date);
        holder.bindDay(dayValue, moodColor);
        View.OnClickListener clickListener = v -> onDayClickListener.onDayClick(date);
        holder.itemView.setOnClickListener(clickListener);
        holder.binding.dayCard.setOnClickListener(clickListener);
    }

    @Override
    // Returns the total number of cells shown in the month grid.
    public int getItemCount() {
        return visibleDates.size();
    }

    // Holds and styles each calendar tile.
    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final ItemDayBinding binding;

        DayViewHolder(ItemDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        // Turns placeholder cells transparent so the calendar aligns cleanly.
        void bindEmpty() {
            binding.dayNumber.setText("");
            binding.dayCard.setCardBackgroundColor(Color.TRANSPARENT);
            binding.dayCard.setCardElevation(0f);
            binding.dayCard.setClickable(false);
            binding.getRoot().setOnClickListener(null);
            binding.dayCard.setOnClickListener(null);
        }

        // Shows the day number and applies either the saved mood color or a default gray.
        void bindDay(String dayValue, String moodColor) {
            binding.dayNumber.setText(String.valueOf(Integer.parseInt(dayValue)));
            binding.dayCard.setCardElevation(4f);
            binding.dayCard.setClickable(true);
            int backgroundColor = moodColor == null ? Color.parseColor("#E5E7EB") : Color.parseColor(moodColor);
            int textColor = isDark(backgroundColor) ? Color.WHITE : Color.parseColor("#111827");
            binding.dayCard.setCardBackgroundColor(backgroundColor);
            binding.dayNumber.setTextColor(textColor);
        }

        // Chooses a readable text color based on how dark the background is.
        private boolean isDark(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness >= 0.35;
        }
    }
}
