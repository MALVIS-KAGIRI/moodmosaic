package com.example.moodmosaic;

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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moodmosaic.databinding.ActivityArtBinding;

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

    // Launches the art screen with the selected month information.
    public static void start(Context context, String monthPrefix, String monthTitle) {
        Intent intent = new Intent(context, ArtActivity.class);
        intent.putExtra(EXTRA_MONTH_PREFIX, monthPrefix);
        intent.putExtra(EXTRA_MONTH_TITLE, monthTitle);
        context.startActivity(intent);
    }

    @Override
    // Reads the selected month, wires the action buttons, and renders the first artwork.
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
        binding.regenerateButton.setOnClickListener(v -> renderArt());
        binding.saveButton.setOnClickListener(v -> saveArtwork(false));
        binding.shareButton.setOnClickListener(v -> saveArtwork(true));

        renderArt();
    }

    // Loads the month's moods and displays a fresh generated bitmap.
    private void renderArt() {
        List<DayMood> moods = databaseHelper.getMoodsForMonth(monthPrefix);
        currentBitmap = generateArt(moods);
        currentImageUri = null;
        binding.artPreview.setImageBitmap(currentBitmap);
    }

    // Draws layered circles, rectangles, and ovals using the saved mood palette.
    private Bitmap generateArt(List<DayMood> moods) {
        Bitmap bitmap = Bitmap.createBitmap(ART_SIZE, ART_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#F8F5F0"));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random((monthPrefix + moods.size()).hashCode());

        // Reuses saved mood colors repeatedly so the final artwork reflects the month's palette.
        for (int i = 0; i < Math.max(24, moods.size() * 3); i++) {
            String colorHex = moods.isEmpty()
                    ? fallbackColor(i)
                    : moods.get(i % moods.size()).getColorHex();
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

        return bitmap;
    }

    // Provides a soft default palette when a month has no saved moods yet.
    private String fallbackColor(int index) {
        String[] colors = {"#D9ED92", "#34A0A4", "#FFB5A7", "#BDB2FF", "#A9DEF9", "#FFD166"};
        return colors[index % colors.length];
    }

    // Saves the current bitmap and optionally launches the Android share sheet.
    private void saveArtwork(boolean shareAfterSave) {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.art_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            currentImageUri = saveBitmapToGallery(currentBitmap);
            String message = getString(R.string.saved_success);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            if (shareAfterSave) {
                shareArtwork();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    // Writes the generated image into the device gallery using MediaStore.
    private Uri saveBitmapToGallery(Bitmap bitmap) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, String.format(Locale.getDefault(), "MoodMosaic-%s.png", monthPrefix));
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
