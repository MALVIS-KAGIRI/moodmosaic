# MoodMosaic

MoodMosaic is an Android mood journal built in Java where users log how they feel using color instead of text. Each day is represented by a colored tile in a monthly grid, and the saved palette can be transformed into abstract artwork at the end of the month.

## Screenshots

### Home Screen
![MoodMosaic Home](C:/Users/USER/Desktop/Screenshot_20260423_092613.png)

### Monthly Art Canvas
![MoodMosaic Art Screen](C:/Users/USER/Desktop/Screenshot_20260423_092730.png)

### Calendar View
![MoodMosaic Calendar](C:/Users/USER/Desktop/Screenshot_20260423_092753.png)

## Features

- Log a daily mood with a single tap using preset colors.
- View moods in a 7-column monthly calendar grid.
- Store mood entries locally with SQLite.
- Generate abstract monthly artwork from saved mood colors.
- Save generated art to the gallery.
- Share generated art with other apps.

## Tech Stack

- Language: Java
- IDE: Android Studio
- Database: SQLite with `SQLiteOpenHelper`
- UI: `RecyclerView`, `GridLayoutManager`, `CardView`, `ConstraintLayout`
- Graphics: `Bitmap`, `Canvas`, `Paint`

## How It Works

### 1. Mood Logging
Users tap a day tile in the monthly grid and choose a preset color. That color is saved as the mood for the selected date.

### 2. Calendar Visualization
Each saved mood is displayed directly on the monthly calendar as a colored square, turning the month into a visual diary.

### 3. Art Generation
The app reads the saved moods for the selected month and uses those colors to draw layered abstract shapes onto a bitmap canvas.

### 4. Save and Share
The generated art can be saved to the device gallery and shared through Android's share sheet.

## Project Structure

```text
app/
  src/main/
    java/com/example/moodmosaic/
      MainActivity.java
      ArtActivity.java
      MoodAdapter.java
      DatabaseHelper.java
      DayMood.java
    res/
      layout/
      drawable/
      values/
```

## Key Files

- `MainActivity.java`: Main calendar screen for browsing months and logging moods.
- `ArtActivity.java`: Generates and displays monthly abstract art.
- `MoodAdapter.java`: Binds the calendar grid and handles day tile visuals.
- `DatabaseHelper.java`: Creates and manages the SQLite database.
- `DayMood.java`: Simple model for storing a day's date and color.

## Setup

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Make sure Android SDK 35 is installed.
4. Run the app on an emulator or Android device.

## Notes

- The app is implemented in Java.
- Mood data is stored locally on-device.
- The design focuses on quick daily interaction and visual storytelling.

## Future Improvements

- Add custom color picker support.
- Add mood history by month.
- Add multiple art styles.
- Add onboarding and export options.

