package com.moodmosaic;

// Compact model used to present saved mood history month by month.
public class MonthSummary {
    private final String monthKey;
    private final int totalEntries;
    private final String topMood;

    public MonthSummary(String monthKey, int totalEntries, String topMood) {
        this.monthKey = monthKey;
        this.totalEntries = totalEntries;
        this.topMood = topMood;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public String getTopMood() {
        return topMood;
    }
}
