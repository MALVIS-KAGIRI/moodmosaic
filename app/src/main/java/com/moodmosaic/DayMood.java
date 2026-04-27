package com.moodmosaic;

// Simple model object that stores one day's mood entry.
public class DayMood {
    private String date;
    private String colorHex;
    private String moodName;

    // Creates a mood record with the saved date, color, and mood label.
    public DayMood(String date, String colorHex, String moodName) {
        this.date = date;
        this.colorHex = colorHex;
        this.moodName = moodName;
    }

    // Returns the day this mood belongs to.
    public String getDate() {
        return date;
    }

    // Returns the stored mood color in hex format.
    public String getColorHex() {
        return colorHex;
    }

    // Returns the human-readable mood tied to the color.
    public String getMoodName() {
        return moodName;
    }

    // Updates the color when the user changes their mood for the day.
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    // Updates the mood label when the day is reclassified.
    public void setMoodName(String moodName) {
        this.moodName = moodName;
    }
}
