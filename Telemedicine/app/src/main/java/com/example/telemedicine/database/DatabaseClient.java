package com.example.telemedicine.database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {

    private static AppDatabase instance; // Singleton instance

    // Get the singleton instance of the database
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) { // Thread-safe initialization
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "user_database" // Database name
                            ).fallbackToDestructiveMigration() // Handle migrations if schema changes
                            .build();
                }
            }
        }
        return instance;
    }
}

