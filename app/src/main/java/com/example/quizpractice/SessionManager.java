package com.example.quizpractice;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "QuizSessionPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_SESSION_START = "sessionStart";
    private static final String KEY_LAST_ACTIVITY = "lastActivity";
    private static final String KEY_SESSION_ID = "sessionId";
    
    private static SessionManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;
    
    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }
    
    /**
     * Create a new user session after successful login
     */
    public void createSession(String userId, String email, String name) {
        Log.d(TAG, "Creating new session for user: " + email);
        
        String sessionId = generateSessionId();
        long currentTime = System.currentTimeMillis();
        
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putLong(KEY_SESSION_START, currentTime);
        editor.putLong(KEY_LAST_ACTIVITY, currentTime);
        editor.putString(KEY_SESSION_ID, sessionId);
        
        editor.apply();
        
        Log.d(TAG, "Session created successfully. Session ID: " + sessionId);
        
        // Log session creation to Firebase
        logSessionToFirebase(userId, sessionId, "LOGIN");
    }
    
    /**
     * Check if user is currently logged in
     */
    public boolean isLoggedIn() {
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        
        if (isLoggedIn) {
            // Check if session is still valid
            if (isSessionValid()) {
                updateLastActivity();
                return true;
            } else {
                Log.w(TAG, "Session expired, logging out user");
                logout();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Get current user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get current user email
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    /**
     * Get current user name
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    /**
     * Get session start time
     */
    public long getSessionStartTime() {
        return prefs.getLong(KEY_SESSION_START, 0);
    }
    
    /**
     * Get session ID
     */
    public String getSessionId() {
        return prefs.getString(KEY_SESSION_ID, null);
    }
    
    /**
     * Get session duration in milliseconds
     */
    public long getSessionDuration() {
        long startTime = getSessionStartTime();
        if (startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateLastActivity() {
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Check if session is still valid (not expired)
     */
    public boolean isSessionValid() {
        long lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0);
        long currentTime = System.currentTimeMillis();
        
        // Session expires after 24 hours of inactivity
        long sessionTimeout = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
        
        return (currentTime - lastActivity) < sessionTimeout;
    }
    
    /**
     * Logout user and clear session
     */
    public void logout() {
        Log.d(TAG, "Logging out user: " + getUserEmail());
        
        String userId = getUserId();
        String sessionId = getSessionId();
        
        // Log session end to Firebase
        if (userId != null && sessionId != null) {
            logSessionToFirebase(userId, sessionId, "LOGOUT");
        }
        
        // Clear all session data
        editor.clear();
        editor.apply();
        
        Log.d(TAG, "Session cleared successfully");
    }
    
    /**
     * Refresh session (extend validity)
     */
    public void refreshSession() {
        if (isLoggedIn()) {
            updateLastActivity();
            Log.d(TAG, "Session refreshed for user: " + getUserEmail());
        }
    }
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * Log session events to Firebase
     */
    private void logSessionToFirebase(String userId, String sessionId, String event) {
        try {
            // Create session log entry
            SessionLog sessionLog = new SessionLog(
                userId,
                sessionId,
                event,
                System.currentTimeMillis(),
                getSessionDuration()
            );
            
            // Save to Firestore
            DbQuery.g_firestore.collection("SESSION_LOGS")
                .add(sessionLog)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Session log saved to Firebase: " + event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save session log: " + e.getMessage());
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error logging session to Firebase: " + e.getMessage());
        }
    }
    
    /**
     * Get session statistics
     */
    public SessionStats getSessionStats() {
        return new SessionStats(
            getSessionStartTime(),
            getSessionDuration(),
            prefs.getLong(KEY_LAST_ACTIVITY, 0)
        );
    }
    
    /**
     * Clear session data without logging (for testing)
     */
    public void clearSessionData() {
        editor.clear();
        editor.apply();
        Log.d(TAG, "Session data cleared");
    }
}
