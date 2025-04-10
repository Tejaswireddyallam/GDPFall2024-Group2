package com.example.telemedicine.model;

public class ChatMessage {
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private String fileUrl;

    public ChatMessage() { }

    // Constructor for text messages
    public ChatMessage(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.fileUrl = null;
    }

    // Constructor for file messages
    public ChatMessage(String senderId, String receiverId, String message, long timestamp, String fileUrl) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.fileUrl = fileUrl;
    }

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getFileUrl() { return fileUrl; }
}

