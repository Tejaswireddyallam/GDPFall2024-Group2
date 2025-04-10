package com.example.telemedicine.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Appointments {
    private String date;
    private String time;
    private String name;
    private String id;
    private String status;
    private String patientId;
    private String doctorId;

    public Appointments(String date, String time, String name, String id, String status, String patientId, String doctorId) {
        this.date = date;
        this.time = time;
        this.name = name;
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    public String getDate() {

        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
        try {
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId){
        this.patientId = patientId;
    }

    public String getPatientId(){
        return patientId;
    }

    public void setDoctorId(String patientId){
        this.doctorId = doctorId;
    }

    public String getDoctorId(){
        return doctorId;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}

