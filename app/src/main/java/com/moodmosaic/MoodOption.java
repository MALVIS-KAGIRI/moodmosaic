package com.moodmosaic;

// Represents a selectable mood choice with a label and color.
public class MoodOption {
    private final String moodName;
    private final String colorHex;

    public MoodOption(String moodName, String colorHex) {
        this.moodName = moodName;
        this.colorHex = colorHex;
    }

    public String getMoodName() {
        return moodName;
    }

    public String getColorHex() {
        return colorHex;
    }
}
