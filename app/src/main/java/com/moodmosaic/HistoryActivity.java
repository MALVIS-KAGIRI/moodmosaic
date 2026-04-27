package com.moodmosaic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.moodmosaic.databinding.ActivityHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Shows month-by-month mood history with quick jump-back into older entries.
public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private DatabaseHelper databaseHelper;
    private final List<MonthSummary> summaries = new ArrayList<>();

    public static void start(Context context) {
        context.startActivity(new Intent(context, HistoryActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        bindHistory();
    }

    // Loads each saved month into a simple readable list.
    private void bindHistory() {
        summaries.clear();
        summaries.addAll(databaseHelper.getMonthSummaries());

        List<String> rows = new ArrayList<>();
        for (MonthSummary summary : summaries) {
            rows.add(getString(
                    R.string.history_row_template,
                    formatMonth(summary.getMonthKey()),
                    summary.getTotalEntries(),
                    summary.getTopMood()
            ));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        binding.historyList.setAdapter(adapter);
        binding.historyList.setOnItemClickListener((parent, view, position, id) -> MainActivity.start(this, summaries.get(position).getMonthKey()));

        binding.emptyHistoryLabel.setText(rows.isEmpty() ? R.string.history_empty : R.string.history_hint);
    }

    // Converts a month key like 2026-04 into a human-readable header.
    private String formatMonth(String monthKey) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey);
            return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return monthKey;
        }
    }
}
