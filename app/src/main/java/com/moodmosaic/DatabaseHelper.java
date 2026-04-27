package com.moodmosaic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Handles all local SQLite storage for daily moods, reusable custom moods, and month summaries.
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "MoodMosaic.db";
    private static final int DB_VERSION = 3;

    public static final String TABLE_NAME = "moods";
    public static final String COL_DATE = "date";
    public static final String COL_COLOR = "color";
    public static final String COL_MOOD_NAME = "mood_name";

    public static final String CUSTOM_MOODS_TABLE = "custom_moods";
    public static final String COL_CUSTOM_NAME = "name";
    public static final String COL_CUSTOM_COLOR = "color";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMoodsTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_DATE + " TEXT PRIMARY KEY, " +
                COL_COLOR + " TEXT, " +
                COL_MOOD_NAME + " TEXT)";
        db.execSQL(createMoodsTable);

        String createCustomMoodsTable = "CREATE TABLE " + CUSTOM_MOODS_TABLE + " (" +
                COL_CUSTOM_NAME + " TEXT, " +
                COL_CUSTOM_COLOR + " TEXT PRIMARY KEY)";
        db.execSQL(createCustomMoodsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_MOOD_NAME + " TEXT DEFAULT 'Mood'");
        }
        if (oldVersion < 3) {
            String createCustomMoodsTable = "CREATE TABLE IF NOT EXISTS " + CUSTOM_MOODS_TABLE + " (" +
                    COL_CUSTOM_NAME + " TEXT, " +
                    COL_CUSTOM_COLOR + " TEXT PRIMARY KEY)";
            db.execSQL(createCustomMoodsTable);
        }
    }

    // Saves or replaces a mood for a specific day.
    public void insertOrUpdateMood(String date, String colorHex, String moodName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, date);
        values.put(COL_COLOR, colorHex);
        values.put(COL_MOOD_NAME, moodName);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Loads the saved day moods for a month key such as 2026-04.
    public List<DayMood> getMoodsForMonth(String monthPrefix) {
        List<DayMood> moods = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COL_DATE, COL_COLOR, COL_MOOD_NAME},
                COL_DATE + " LIKE ?",
                new String[]{monthPrefix + "%"},
                null,
                null,
                COL_DATE + " ASC")) {

            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLOR));
                String moodName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MOOD_NAME));
                moods.add(new DayMood(date, color, moodName == null ? "Mood" : moodName));
            }
        }

        return moods;
    }

    // Returns one summary row for each month that contains saved moods.
    public List<MonthSummary> getMonthSummaries() {
        List<MonthSummary> summaries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String query = "SELECT substr(" + COL_DATE + ", 1, 7) AS month_key, COUNT(*) AS total " +
                "FROM " + TABLE_NAME + " GROUP BY month_key ORDER BY month_key DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                String monthKey = cursor.getString(cursor.getColumnIndexOrThrow("month_key"));
                int totalEntries = cursor.getInt(cursor.getColumnIndexOrThrow("total"));
                summaries.add(new MonthSummary(monthKey, totalEntries, getTopMoodForMonth(monthKey)));
            }
        }

        return summaries;
    }

    // Finds the most common mood name for a given month.
    public String getTopMoodForMonth(String monthPrefix) {
        SQLiteDatabase db = getReadableDatabase();

        String query = "SELECT " + COL_MOOD_NAME + ", COUNT(*) AS total " +
                "FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ? " +
                "GROUP BY " + COL_MOOD_NAME + " ORDER BY total DESC, " + COL_MOOD_NAME + " ASC LIMIT 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{monthPrefix + "%"})) {
            if (cursor.moveToFirst()) {
                String moodName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MOOD_NAME));
                return moodName == null ? "Mood" : moodName;
            }
        }

        return "No moods yet";
    }

    // Saves a custom mood so it can be reused later from the picker.
    public void insertOrUpdateCustomMood(String moodName, String colorHex) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CUSTOM_NAME, moodName);
        values.put(COL_CUSTOM_COLOR, colorHex);
        db.insertWithOnConflict(CUSTOM_MOODS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Loads all reusable custom moods in alphabetical order.
    public List<MoodOption> getCustomMoods() {
        List<MoodOption> customMoods = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                CUSTOM_MOODS_TABLE,
                new String[]{COL_CUSTOM_NAME, COL_CUSTOM_COLOR},
                null,
                null,
                null,
                null,
                COL_CUSTOM_NAME + " COLLATE NOCASE ASC")) {

            while (cursor.moveToNext()) {
                String moodName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOM_NAME));
                String colorHex = cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOM_COLOR));
                customMoods.add(new MoodOption(moodName, colorHex));
            }
        }

        return customMoods;
    }
}
