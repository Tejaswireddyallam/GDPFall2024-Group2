package com.example.telemedicine.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "userInfo",
        indices = {@Index(value = {"email"}, unique = true)}
)
public class User {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SNo")
    public int SNo;

    @ColumnInfo(name = "userID")
    public int userID;

    @ColumnInfo(name = "fullName")
    public String fullName;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "token")
    public String token;

    @ColumnInfo(name = "contactInfo")
    public int contactInfo;

    @ColumnInfo(name = "role")
    public String role;

    @ColumnInfo(name = "isVerified")
    public boolean isVerified = false;

}
