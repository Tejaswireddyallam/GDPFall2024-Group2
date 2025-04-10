package com.example.telemedicine.model;

public class Chat {
    private String chatId;
    private String name;
    private String userId;
    private Boolean isDoctor;

    public Chat(){}

    public Chat(String chatId, String name, String userId, Boolean isDoctor) {
        this.chatId = chatId;
        this.name = name;
        this.userId = userId;
        this.isDoctor = isDoctor;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getIsDoctor() {
        return isDoctor;
    }

    public void setIsDoctor(Boolean isDoctor) {
        this.isDoctor = isDoctor;
    }
}
