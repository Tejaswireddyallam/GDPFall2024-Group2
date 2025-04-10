package com.example.telemedicine.model;

public class Pharmacy {
    private int pharmacyID;
    private String name;
    private String location;
    private String contactInfo;

    public Pharmacy(int pharmacyID, String name, String location, String contactInfo) {
        this.pharmacyID = pharmacyID;
        this.name = name;
        this.location = location;
        this.contactInfo = contactInfo;
    }

    public Pharmacy() {

    }

    public int getPharmacyID() { return pharmacyID; }
    public void setPharmacyID(int pharmacyID) {
        this.pharmacyID = pharmacyID;
    }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getContactInfo() { return contactInfo; }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}

