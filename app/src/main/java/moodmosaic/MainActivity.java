package com.example.moodmosaic;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.moodmosaic.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Main screen that shows the monthly mood calendar and launches mood picking.
public class MainActivity extends AppCompatActivity {

    // Preset mood colors used in the lightweight picker dialog.
    private static final String[] PRESET_COLORS = {
            "#F94144", "#F3722C", "#F9C74F", "#90BE6D",
            "#43AA8B", "#577590", "#277DA1", "#9B5DE5",
            "#F15BB5", "#00BBF9", "#00F5D4", "#FFDDD2"
    };

    private ActivityMainBinding binding;
    private DatabaseHelper databaseHelper;
    private MoodAdapter moodAdapter;
    private Calendar visibleMonth;

    @Override
    // Sets up the month view, grid adapter, navigation, and art button.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        visibleMonth = Calendar.getInstance();
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);

        moodAdapter = new MoodAdapter(buildVisibleDates(), this::showColorPicker);
        binding.moodRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        binding.moodRecyclerView.setAdapter(moodAdapter);

        binding.previousMonthButton.setOnClickListener(v -> moveMonthBy(-1));
        binding.nextMonthButton.setOnClickListener(v -> moveMonthBy(1));
        binding.artButton.setOnClickListener(v -> ArtActivity.start(this, getMonthPrefix(), getMonthTitle()));

        refreshMonth();
    }

    // Moves backward or forward by one month, then rebuilds the grid for that month.
    private void moveMonthBy(int delta) {
        visibleMonth.add(Calendar.MONTH, delta);
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
        moodAdapter = new MoodAdapter(buildVisibleDates(), this::showColorPicker);
        binding.moodRecyclerView.setAdapter(moodAdapter);
        refreshMonth();
    }

    // Updates the title and reloads any saved moods for the visible month.
    private void refreshMonth() {
        binding.monthTitle.setText(getMonthTitle());
        moodAdapter.submitMoods(databaseHelper.getMoodsForMonth(getMonthPrefix()));
    }

    // Builds a calendar list with leading blank cells so the month aligns to a 7-column grid.
    private List<String> buildVisibleDates() {
        List<String> dates = new ArrayList<>();
        Calendar monthCursor = (Calendar) visibleMonth.clone();
        int firstDayOffset = (monthCursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int i = 0; i < firstDayOffset; i++) {
            dates.add("");
        }

        int daysInMonth = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int day = 1; day <= daysInMonth; day++) {
            monthCursor.set(Calendar.DAY_OF_MONTH, day);
            dates.add(dateFormat.format(monthCursor.getTime()));
        }
        return dates;
    }

    // Opens a simple dialog with preset color swatches and saves the chosen mood.
    private void showColorPicker(String date) {
        GridLayout gridLayout = new GridLayout(this);
        int padding = dpToPx(20);
        gridLayout.setColumnCount(4);
        gridLayout.setUseDefaultMargins(true);
        gridLayout.setPadding(padding, padding, padding, padding);

        // Creates the color tiles shown in the picker.
        for (String presetColor : PRESET_COLORS) {
            View colorSwatch = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(56);
            params.height = dpToPx(56);
            colorSwatch.setLayoutParams(params);
            colorSwatch.setBackgroundColor(android.graphics.Color.parseColor(presetColor));
            gridLayout.addView(colorSwatch);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_color)
                .setView(gridLayout)
                .setNegativeButton(R.string.cancel, null)
                .create();

        // Connects each swatch to a database save action for the tapped day.
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            child.setOnClickListener(v -> {
                String color = PRESET_COLORS[gridLayout.indexOfChild(v)];
                databaseHelper.insertOrUpdateMood(date, color);
                refreshMonth();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    // Returns the current month key used by the database query.
    private String getMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(visibleMonth.getTime());
    }

    // Formats the visible month for the screen header.
    private String getMonthTitle() {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(visibleMonth.getTime());
    }

    // Converts dp spacing into pixels for consistent sizing on different screens.
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
