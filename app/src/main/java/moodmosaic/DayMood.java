package com.example.moodmosaic;

// Simple model object that stores one day's mood entry.
public class DayMood {
    private String date;
    private String colorHex;

    // Creates a mood record with the saved date and color.
    public DayMood(String date, String colorHex) {
        this.date = date;
        this.colorHex = colorHex;
    }

    // Returns the day this mood belongs to.
    public String getDate() {
        return date;
    }

    // Returns the stored mood color in hex format.
    public String getColorHex() {
        return colorHex;
    }

    // Updates the color when the user changes their mood for the day.
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
