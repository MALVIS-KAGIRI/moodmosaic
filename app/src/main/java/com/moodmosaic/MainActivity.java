package com.moodmosaic;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.moodmosaic.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Main screen that shows the monthly mood calendar and launches mood picking.
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "mood_mosaic_prefs";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String EXTRA_INITIAL_MONTH = "extra_initial_month";

    private final List<MoodOption> presetMoods = new ArrayList<>();
    private final List<MoodOption> customMoods = new ArrayList<>();

    private ActivityMainBinding binding;
    private DatabaseHelper databaseHelper;
    private MoodAdapter moodAdapter;
    private Calendar visibleMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        visibleMonth = Calendar.getInstance();
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
        applyInitialMonth();

        seedPresetMoods();
        reloadCustomMoods();

        moodAdapter = new MoodAdapter(buildVisibleDates(), this::showColorPicker);
        binding.moodRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        binding.moodRecyclerView.setAdapter(moodAdapter);

        binding.previousMonthButton.setOnClickListener(v -> moveMonthBy(-1));
        binding.nextMonthButton.setOnClickListener(v -> moveMonthBy(1));
        binding.artButton.setOnClickListener(v -> ArtActivity.start(this, getMonthPrefix(), getMonthTitle()));
        binding.historyButton.setOnClickListener(v -> HistoryActivity.start(this));
        binding.exportButton.setOnClickListener(v -> showExportOptions());
        binding.infoButton.setOnClickListener(v -> showOnboardingDialog(false));

        refreshMonth();
        maybeShowOnboarding();
    }

    // Opens a month returned from the history screen if one was provided.
    private void applyInitialMonth() {
        String initialMonth = getIntent().getStringExtra(EXTRA_INITIAL_MONTH);
        if (initialMonth == null || initialMonth.length() != 7) {
            return;
        }
        try {
            Calendar parsed = Calendar.getInstance();
            parsed.setTime(new SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(initialMonth));
            visibleMonth = parsed;
            visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
        } catch (Exception ignored) {
        }
    }

    // Creates the built-in moods shown at the top of the picker.
    private void seedPresetMoods() {
        presetMoods.clear();
        presetMoods.add(new MoodOption("Energized", "#F94144"));
        presetMoods.add(new MoodOption("Optimistic", "#F3722C"));
        presetMoods.add(new MoodOption("Playful", "#F9C74F"));
        presetMoods.add(new MoodOption("Grounded", "#90BE6D"));
        presetMoods.add(new MoodOption("Balanced", "#43AA8B"));
        presetMoods.add(new MoodOption("Reflective", "#577590"));
        presetMoods.add(new MoodOption("Focused", "#277DA1"));
        presetMoods.add(new MoodOption("Dreamy", "#9B5DE5"));
        presetMoods.add(new MoodOption("Romantic", "#F15BB5"));
        presetMoods.add(new MoodOption("Clear", "#00BBF9"));
        presetMoods.add(new MoodOption("Light", "#00F5D4"));
        presetMoods.add(new MoodOption("Gentle", "#FFDDD2"));
    }

    // Loads saved custom moods so they can be reused without rebuilding the color.
    private void reloadCustomMoods() {
        customMoods.clear();
        customMoods.addAll(databaseHelper.getCustomMoods());
    }

    // Shows onboarding only once on first launch.
    private void maybeShowOnboarding() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_ONBOARDING_SEEN, false)) {
            showOnboardingDialog(true);
            preferences.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
        }
    }

    // Explains mood logging, history, and art generation.
    private void showOnboardingDialog(boolean firstLaunch) {
        String message = getString(
                R.string.onboarding_message,
                getString(R.string.onboarding_step_one),
                getString(R.string.onboarding_step_two),
                getString(R.string.onboarding_step_three),
                getString(R.string.onboarding_step_four)
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(firstLaunch ? R.string.onboarding_title : R.string.help_title)
                .setMessage(message)
                .setPositiveButton(firstLaunch ? R.string.start_journaling : R.string.close, null);

        if (!firstLaunch) {
            builder.setNeutralButton(R.string.view_history, (dialog, which) -> HistoryActivity.start(this));
        }

        builder.show();
    }

    // Moves the visible month backward or forward.
    private void moveMonthBy(int delta) {
        visibleMonth.add(Calendar.MONTH, delta);
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
        moodAdapter = new MoodAdapter(buildVisibleDates(), this::showColorPicker);
        binding.moodRecyclerView.setAdapter(moodAdapter);
        refreshMonth();
    }

    // Updates the month header and the saved mood summary for the current month.
    private void refreshMonth() {
        String monthPrefix = getMonthPrefix();
        List<DayMood> moods = databaseHelper.getMoodsForMonth(monthPrefix);
        binding.monthTitle.setText(getMonthTitle());
        moodAdapter.submitMoods(moods);

        if (moods.isEmpty()) {
            binding.streakLabel.setText(R.string.tap_any_day);
            binding.streakHint.setText(R.string.visual_hint);
        } else {
            binding.streakLabel.setText(getString(R.string.month_summary_label, moods.size()));
            binding.streakHint.setText(getString(R.string.top_mood_hint, databaseHelper.getTopMoodForMonth(monthPrefix)));
        }
    }

    // Builds the month grid with leading blanks so dates align to weekdays.
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

    // Opens the mood picker with preset moods, saved custom moods, and a custom mood builder.
    private void showColorPicker(String date) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, padding);
        scrollView.addView(container);

        TextView presetLabel = buildSectionTitle(getString(R.string.preset_moods));
        container.addView(presetLabel);

        GridLayout presetGrid = new GridLayout(this);
        presetGrid.setColumnCount(2);
        presetGrid.setUseDefaultMargins(true);
        container.addView(presetGrid);

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        for (MoodOption moodOption : presetMoods) {
            TextView optionView = buildMoodOptionView(moodOption);
            optionView.setOnClickListener(v -> {
                databaseHelper.insertOrUpdateMood(date, moodOption.getColorHex(), moodOption.getMoodName());
                refreshMonth();
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
            });
            presetGrid.addView(optionView);
        }

        if (!customMoods.isEmpty()) {
            TextView savedLabel = buildSectionTitle(getString(R.string.saved_custom_moods));
            savedLabel.setPadding(0, dpToPx(18), 0, dpToPx(8));
            container.addView(savedLabel);

            GridLayout savedGrid = new GridLayout(this);
            savedGrid.setColumnCount(2);
            savedGrid.setUseDefaultMargins(true);
            container.addView(savedGrid);

            for (MoodOption moodOption : customMoods) {
                TextView optionView = buildMoodOptionView(moodOption);
                optionView.setOnClickListener(v -> {
                    databaseHelper.insertOrUpdateMood(date, moodOption.getColorHex(), moodOption.getMoodName());
                    refreshMonth();
                    if (dialogHolder[0] != null) {
                        dialogHolder[0].dismiss();
                    }
                });
                savedGrid.addView(optionView);
            }
        }

        TextView customLabel = buildSectionTitle(getString(R.string.create_custom_mood));
        customLabel.setPadding(0, dpToPx(18), 0, dpToPx(8));
        container.addView(customLabel);

        EditText moodNameInput = new EditText(this);
        moodNameInput.setHint(R.string.custom_mood_hint);
        moodNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        moodNameInput.setBackgroundResource(android.R.drawable.edit_text);
        container.addView(moodNameInput);

        TextView preview = new TextView(this);
        preview.setGravity(Gravity.CENTER);
        preview.setPadding(0, dpToPx(14), 0, dpToPx(14));
        preview.setText(R.string.custom_preview);
        preview.setTextSize(16f);
        preview.setTextColor(Color.WHITE);
        container.addView(preview);

        SeekBar redSeek = buildColorSeekBar(container, getString(R.string.red_channel));
        SeekBar greenSeek = buildColorSeekBar(container, getString(R.string.green_channel));
        SeekBar blueSeek = buildColorSeekBar(container, getString(R.string.blue_channel));

        redSeek.setProgress(127);
        greenSeek.setProgress(170);
        blueSeek.setProgress(255);

        Runnable updatePreview = () -> {
            int color = Color.rgb(redSeek.getProgress(), greenSeek.getProgress(), blueSeek.getProgress());
            preview.setBackgroundColor(color);
            preview.setText(toHex(color));
            preview.setTextColor(isDark(color) ? Color.WHITE : Color.parseColor("#1D2833"));
        };

        SeekBar.OnSeekBarChangeListener listener = new SimpleSeekListener(updatePreview);
        redSeek.setOnSeekBarChangeListener(listener);
        greenSeek.setOnSeekBarChangeListener(listener);
        blueSeek.setOnSeekBarChangeListener(listener);
        updatePreview.run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.pick_color_for_day, formatDay(date)))
                .setView(scrollView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_custom_mood, (d, which) -> {
                    String moodName = moodNameInput.getText().toString().trim();
                    if (moodName.isEmpty()) {
                        moodName = getString(R.string.custom_mood_default);
                    }
                    String colorHex = toHex(Color.rgb(redSeek.getProgress(), greenSeek.getProgress(), blueSeek.getProgress()));
                    databaseHelper.insertOrUpdateCustomMood(moodName, colorHex);
                    reloadCustomMoods();
                    databaseHelper.insertOrUpdateMood(date, colorHex, moodName);
                    refreshMonth();
                })
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }

    // Builds a section title inside the picker dialog.
    private TextView buildSectionTitle(String title) {
        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setTextColor(Color.parseColor("#1D2833"));
        textView.setTextSize(16f);
        textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        return textView;
    }

    // Creates a tappable card view for a mood option.
    private TextView buildMoodOptionView(MoodOption moodOption) {
        TextView optionView = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dpToPx(148);
        params.height = dpToPx(72);
        optionView.setLayoutParams(params);
        optionView.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        optionView.setGravity(Gravity.CENTER_VERTICAL);
        optionView.setText(getString(R.string.mood_option_template, moodOption.getMoodName(), moodOption.getColorHex()));
        int color = Color.parseColor(moodOption.getColorHex());
        optionView.setBackgroundColor(color);
        optionView.setTextColor(isDark(color) ? Color.WHITE : Color.parseColor("#1D2833"));
        return optionView;
    }

    // Creates a labeled RGB slider used for building custom moods.
    private SeekBar buildColorSeekBar(LinearLayout container, String label) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setPadding(0, dpToPx(12), 0, 0);
        textView.setTextColor(Color.parseColor("#5A6472"));
        container.addView(textView);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(255);
        container.addView(seekBar);
        return seekBar;
    }

    // Opens export actions for CSV and summary sharing.
    private void showExportOptions() {
        String[] options = {
                getString(R.string.export_csv),
                getString(R.string.share_text_summary)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.export_options)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportMonthCsv();
                    } else {
                        shareMonthSummary();
                    }
                })
                .show();
    }

    // Exports the visible month as CSV.
    private void exportMonthCsv() {
        List<DayMood> moods = databaseHelper.getMoodsForMonth(getMonthPrefix());
        if (moods.isEmpty()) {
            Toast.makeText(this, R.string.no_moods_to_export, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("date,mood,color\n");
        for (DayMood mood : moods) {
            csv.append(mood.getDate()).append(',')
                    .append(escapeCsv(mood.getMoodName())).append(',')
                    .append(mood.getColorHex()).append('\n');
        }

        try {
            Uri uri = createDownloadFile(String.format(Locale.getDefault(), "MoodMosaic-%s.csv", getMonthPrefix()), "text/csv");
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Unable to open export stream.");
                }
                outputStream.write(csv.toString().getBytes());
            }
            Toast.makeText(this, R.string.csv_exported, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    // Shares a text summary of the visible month.
    private void shareMonthSummary() {
        List<DayMood> moods = databaseHelper.getMoodsForMonth(getMonthPrefix());
        String summary = getString(
                R.string.share_summary_template,
                getMonthTitle(),
                moods.size(),
                databaseHelper.getTopMoodForMonth(getMonthPrefix())
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_summary)));
    }

    // Creates a file in Downloads using scoped storage.
    private Uri createDownloadFile(String displayName, String mimeType) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MoodMosaic");
        }

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Unable to create export file.");
        }
        return uri;
    }

    private String getMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(visibleMonth.getTime());
    }

    private String getMonthTitle() {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(visibleMonth.getTime());
    }

    private String formatDay(String date) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.getTime());
        } catch (Exception e) {
            return date;
        }
    }

    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }

    private String toHex(int color) {
        return String.format(Locale.getDefault(), "#%06X", 0xFFFFFF & color);
    }

    private boolean isDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.35;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        private final Runnable onUpdate;

        SimpleSeekListener(Runnable onUpdate) {
            this.onUpdate = onUpdate;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            onUpdate.run();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, MainActivity.class));
    }

    public static void start(Context context, String monthKey) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_INITIAL_MONTH, monthKey);
        context.startActivity(intent);
    }
}
