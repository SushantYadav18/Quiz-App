package com.example.quizpractice;

import java.util.Date;

public class SessionLog {
    private String userId;
    private String sessionId;
    private String event; // LOGIN, LOGOUT, SESSION_EXPIRED
    private long timestamp;
    private long sessionDuration;
    private String deviceInfo;
    private String appVersion;
    
    // Required empty constructor for Firestore
    public SessionLog() {}
    
    public SessionLog(String userId, String sessionId, String event, long timestamp, long sessionDuration) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.event = event;
        this.timestamp = timestamp;
        this.sessionDuration = sessionDuration;
        this.deviceInfo = android.os.Build.MODEL;
        this.appVersion = "1.0"; // You can get this from BuildConfig
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getEvent() {
        return event;
    }
    
    public void setEvent(String event) {
        this.event = event;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getSessionDuration() {
        return sessionDuration;
    }
    
    public void setSessionDuration(long sessionDuration) {
        this.sessionDuration = sessionDuration;
    }
    
    public String getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }
    
    public String getFormattedTimestamp() {
        return new Date(timestamp).toString();
    }
    
    public String getFormattedSessionDuration() {
        long seconds = sessionDuration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
