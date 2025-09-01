package com.example.quizpractice;

import java.util.Date;

public class SessionStats {
    private long sessionStartTime;
    private long sessionDuration;
    private long lastActivityTime;
    
    public SessionStats(long sessionStartTime, long sessionDuration, long lastActivityTime) {
        this.sessionStartTime = sessionStartTime;
        this.sessionDuration = sessionDuration;
        this.lastActivityTime = lastActivityTime;
    }
    
    // Getters
    public long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public long getSessionDuration() {
        return sessionDuration;
    }
    
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    // Formatted getters
    public Date getSessionStartDate() {
        return new Date(sessionStartTime);
    }
    
    public Date getLastActivityDate() {
        return new Date(lastActivityTime);
    }
    
    public String getFormattedSessionStart() {
        return new Date(sessionStartTime).toString();
    }
    
    public String getFormattedLastActivity() {
        return new Date(lastActivityTime).toString();
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
    
    public String getFormattedTimeSinceLastActivity() {
        long timeSince = System.currentTimeMillis() - lastActivityTime;
        long seconds = timeSince / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm ago", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm ago", minutes);
        } else {
            return String.format("%ds ago", seconds);
        }
    }
    
    public boolean isSessionActive() {
        // Consider session active if last activity was within last 5 minutes
        long fiveMinutes = 5 * 60 * 1000;
        return (System.currentTimeMillis() - lastActivityTime) < fiveMinutes;
    }
    
    public long getTimeUntilSessionExpiry() {
        // Session expires after 24 hours of inactivity
        long sessionTimeout = 24 * 60 * 60 * 1000;
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        return Math.max(0, sessionTimeout - timeSinceLastActivity);
    }
    
    public String getFormattedTimeUntilExpiry() {
        long timeUntil = getTimeUntilSessionExpiry();
        long seconds = timeUntil / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm remaining", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm remaining", minutes);
        } else {
            return String.format("%ds remaining", seconds);
        }
    }
}
