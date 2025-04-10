package com.example.telemedicine.model;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MedicalRecord {
    private String recordId, patientId, fileName, fileUrl, timestamp;

    public MedicalRecord(String recordId, String patientId, String fileName, String fileUrl, String date) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.timestamp = date;
    }

    public String getRecordId() { return recordId; }
    public String getPatientId() { return patientId; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public String getFormattedTimestamp() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd yyyy, HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid Date";
        }
    }
}

