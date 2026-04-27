package com.moodmosaic;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.moodmosaic.databinding.ActivityArtBinding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// Secondary screen that turns the month's mood colors into abstract artwork.
public class ArtActivity extends AppCompatActivity {

    private static final String EXTRA_MONTH_PREFIX = "extra_month_prefix";
    private static final String EXTRA_MONTH_TITLE = "extra_month_title";
    private static final int ART_SIZE = 800;

    private ActivityArtBinding binding;
    private DatabaseHelper databaseHelper;
    private String monthPrefix;
    private String monthTitle;
    private Bitmap currentBitmap;
    private Uri currentImageUri;
    private int selectedStyleIndex;

    // Launches the art screen with the selected month information.
    public static void start(Context context, String monthPrefix, String monthTitle) {
        Intent intent = new Intent(context, ArtActivity.class);
        intent.putExtra(EXTRA_MONTH_PREFIX, monthPrefix);
        intent.putExtra(EXTRA_MONTH_TITLE, monthTitle);
        context.startActivity(intent);
    }

    @Override
    // Reads the selected month, wires the style selector and action buttons, then renders the first artwork.
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        monthPrefix = getIntent().getStringExtra(EXTRA_MONTH_PREFIX);
        monthTitle = getIntent().getStringExtra(EXTRA_MONTH_TITLE);
        if (monthPrefix == null) {
            monthPrefix = "";
        }
        if (monthTitle == null) {
            monthTitle = getString(R.string.app_name);
        }

        binding.artTitle.setText(monthTitle);
        bindStyleSelector();
        binding.regenerateButton.setOnClickListener(v -> renderArt());
        binding.saveButton.setOnClickListener(v -> saveArtwork(false));
        binding.shareButton.setOnClickListener(v -> saveArtwork(true));
        binding.exportButton.setOnClickListener(v -> showExportOptions());

        renderArt();
    }

    // Populates the drop-down with the available visual styles.
    private void bindStyleSelector() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.art_styles,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.styleSpinner.setAdapter(adapter);
        binding.styleSpinner.setSelection(0);
        binding.styleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStyleIndex = position;
                renderArt();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Loads the month's moods and displays a fresh generated bitmap.
    private void renderArt() {
        List<DayMood> moods = databaseHelper.getMoodsForMonth(monthPrefix);
        currentBitmap = generateArt(moods, selectedStyleIndex);
        currentImageUri = null;
        binding.artPreview.setImageBitmap(currentBitmap);
    }

    // Routes generation through the currently selected art style.
    private Bitmap generateArt(List<DayMood> moods, int styleIndex) {
        Bitmap bitmap = Bitmap.createBitmap(ART_SIZE, ART_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#F8F5F0"));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random((monthPrefix + moods.size() + styleIndex).hashCode());

        switch (styleIndex) {
            case 1:
                drawMosaicStyle(canvas, paint, moods, random);
                break;
            case 2:
                drawBandsStyle(canvas, paint, moods, random);
                break;
            default:
                drawConstellationStyle(canvas, paint, moods, random);
                break;
        }

        return bitmap;
    }

    // Original layered-shape style with circles, rectangles, and ovals.
    private void drawConstellationStyle(Canvas canvas, Paint paint, List<DayMood> moods, Random random) {
        for (int i = 0; i < Math.max(24, moods.size() * 3); i++) {
            String colorHex = resolvePaletteColor(moods, i);
            paint.setColor(Color.parseColor(colorHex));
            paint.setAlpha(120 + random.nextInt(100));

            float left = random.nextFloat() * ART_SIZE;
            float top = random.nextFloat() * ART_SIZE;
            float width = 60 + random.nextFloat() * 220;
            float height = 60 + random.nextFloat() * 220;

            switch (i % 3) {
                case 0:
                    canvas.drawCircle(left, top, 20 + random.nextFloat() * 120, paint);
                    break;
                case 1:
                    canvas.drawRect(left, top, Math.min(ART_SIZE, left + width), Math.min(ART_SIZE, top + height), paint);
                    break;
                default:
                    canvas.drawOval(left, top, Math.min(ART_SIZE, left + width), Math.min(ART_SIZE, top + height), paint);
                    break;
            }
        }
    }

    // Grid-based style that turns the month into a patchwork mosaic.
    private void drawMosaicStyle(Canvas canvas, Paint paint, List<DayMood> moods, Random random) {
        int columns = 8;
        int rows = 8;
        float cellSize = ART_SIZE / (float) columns;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                String colorHex = resolvePaletteColor(moods, row * columns + col);
                paint.setColor(Color.parseColor(colorHex));
                paint.setAlpha(160 + random.nextInt(70));
                float left = col * cellSize;
                float top = row * cellSize;
                canvas.drawRoundRect(left + 8, top + 8, left + cellSize - 8, top + cellSize - 8, 28, 28, paint);
            }
        }
    }

    // Ribbon style made of sweeping translucent bands and circles.
    private void drawBandsStyle(Canvas canvas, Paint paint, List<DayMood> moods, Random random) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < Math.max(12, moods.size() * 2); i++) {
            String colorHex = resolvePaletteColor(moods, i);
            paint.setColor(Color.parseColor(colorHex));
            paint.setAlpha(115 + random.nextInt(80));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(30 + random.nextInt(46));
            float startY = random.nextFloat() * ART_SIZE;
            float endY = random.nextFloat() * ART_SIZE;
            canvas.drawLine(0, startY, ART_SIZE, endY, paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(random.nextFloat() * ART_SIZE, random.nextFloat() * ART_SIZE, 16 + random.nextFloat() * 60, paint);
        }
    }

    // Reuses saved mood colors repeatedly so the final artwork reflects the month's palette.
    private String resolvePaletteColor(List<DayMood> moods, int index) {
        return moods.isEmpty() ? fallbackColor(index) : moods.get(index % moods.size()).getColorHex();
    }

    // Provides a soft default palette when a month has no saved moods yet.
    private String fallbackColor(int index) {
        String[] colors = {"#D9ED92", "#34A0A4", "#FFB5A7", "#BDB2FF", "#A9DEF9", "#FFD166"};
        return colors[index % colors.length];
    }

    // Opens export actions for the generated art and month data.
    private void showExportOptions() {
        String[] options = {
                getString(R.string.export_png),
                getString(R.string.export_csv),
                getString(R.string.share_art)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.export_options)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        saveArtwork(false);
                    } else if (which == 1) {
                        exportMonthCsv();
                    } else {
                        saveArtwork(true);
                    }
                })
                .show();
    }

    // Saves the current bitmap and optionally launches the Android share sheet.
    private void saveArtwork(boolean shareAfterSave) {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.art_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            currentImageUri = saveBitmapToGallery(currentBitmap);
            Toast.makeText(this, getString(R.string.saved_success), Toast.LENGTH_SHORT).show();
            if (shareAfterSave) {
                shareArtwork();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    // Exports the current month as a CSV file from the art screen.
    private void exportMonthCsv() {
        List<DayMood> moods = databaseHelper.getMoodsForMonth(monthPrefix);
        if (moods.isEmpty()) {
            Toast.makeText(this, R.string.no_moods_to_export, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("date,mood,color\n");
        for (DayMood mood : moods) {
            csv.append(mood.getDate()).append(',')
                    .append("\"").append(mood.getMoodName().replace("\"", "\"\"")).append("\"").append(',')
                    .append(mood.getColorHex()).append('\n');
        }

        try {
            Uri uri = createDownloadFile(String.format(Locale.getDefault(), "MoodMosaic-%s.csv", monthPrefix), "text/csv");
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Unable to write export.");
                }
                outputStream.write(csv.toString().getBytes());
            }
            Toast.makeText(this, R.string.csv_exported, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    // Writes the generated image into the device gallery using MediaStore.
    private Uri saveBitmapToGallery(Bitmap bitmap) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, String.format(Locale.getDefault(), "MoodMosaic-%s-%s.png", monthPrefix, getStyleSlug()));
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MoodMosaic");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Unable to create gallery entry.");
        }

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IOException("Unable to write image data.");
            }
        }

        return uri;
    }

    // Creates a file in Downloads using MediaStore so the app can export without legacy storage permissions.
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

    // Converts the selected style into a short filename-safe suffix.
    private String getStyleSlug() {
        String[] slugs = {"constellation", "mosaic", "bands"};
        return slugs[Math.min(selectedStyleIndex, slugs.length - 1)];
    }

    // Shares the last saved image with other apps.
    private void shareArtwork() {
        if (currentImageUri == null) {
            Toast.makeText(this, R.string.save_before_share, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, currentImageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_art)));
    }
}
