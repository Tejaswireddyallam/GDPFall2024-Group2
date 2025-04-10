package com.example.telemedicine.database;

import android.app.Application;

import com.android.volley.RequestQueue;

public class MyApp extends Application {

    private static AppDatabase appDatabase;
    private static RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the database
        appDatabase = DatabaseClient.getInstance(this);
    }

    public static void setRequestQueue(RequestQueue queue) {
        requestQueue = queue;
    }

    public static RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public static AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
