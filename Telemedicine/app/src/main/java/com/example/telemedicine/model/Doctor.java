package com.example.telemedicine.model;

public class Doctor {
    private String id;
    private String name;
    private String specialization;
    private String qualification;
    private String availability;
    private String profilePictureUrl;

    // Constructor
    public Doctor(String id, String name, String specialization, String qualification, String availability, String profilePictureUrl) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.qualification = qualification;
        this.availability = availability;
        this.profilePictureUrl = profilePictureUrl;
    }

    public Doctor() { }

    public String getName() {
        return name;
    }

    public String getId() {
        return  id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}

