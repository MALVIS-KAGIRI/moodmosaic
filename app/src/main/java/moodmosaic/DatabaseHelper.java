package com.example.moodmosaic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Handles all local SQLite storage for daily mood colors.
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "MoodMosaic.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME = "moods";
    public static final String COL_DATE = "date";
    public static final String COL_COLOR = "color";

    // Opens or creates the app database.
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    // Builds the moods table the first time the app runs.
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_DATE + " TEXT PRIMARY KEY, " +
                COL_COLOR + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    // Recreates the table if the database version changes.
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Saves a mood for a date, replacing the old one if that day already exists.
    public void insertOrUpdateMood(String date, String colorHex) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, date);
        values.put(COL_COLOR, colorHex);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Loads every mood saved for the requested month prefix like "2026-04".
    public List<DayMood> getMoodsForMonth(String monthPrefix) {
        List<DayMood> moods = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Queries rows for the month and returns them in calendar order.
        try (Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COL_DATE, COL_COLOR},
                COL_DATE + " LIKE ?",
                new String[]{monthPrefix + "%"},
                null,
                null,
                COL_DATE + " ASC")) {

            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLOR));
                moods.add(new DayMood(date, color));
            }
        }

        return moods;
    }
}
