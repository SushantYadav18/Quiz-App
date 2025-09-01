package com.example.quizpractice;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProgressManager {
    private static final String TAG = "UserProgressManager";
    private static final String PREF_NAME = "UserProgressPrefs";
    private static final String KEY_CATEGORY_PROGRESS = "category_progress_";
    private static final String KEY_TEST_COMPLETION = "test_completion_";
    
    private static UserProgressManager instance;
    private SharedPreferences prefs;
    private FirebaseFirestore firestore;
    private Context context;
    
    private UserProgressManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    public static synchronized UserProgressManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserProgressManager(context);
        }
        return instance;
    }
    
    /**
     * Save test completion result
     */
    public void saveTestCompletion(String categoryId, String testId, int score, int maxScore) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }
        
        // Calculate percentage
        int percentage = (score * 100) / maxScore;
        
        // Save to local preferences
        String key = KEY_TEST_COMPLETION + categoryId + "_" + testId;
        prefs.edit().putInt(key, percentage).apply();
        
        // Save to Firebase
        DocumentReference userDoc = firestore.collection("USERS").document(userId);
        DocumentReference progressDoc = userDoc.collection("PROGRESS").document(categoryId);
        
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("CATEGORY_ID", categoryId);
        progressData.put("LAST_UPDATED", System.currentTimeMillis());
        
        // Update test completion
        Map<String, Object> testData = new HashMap<>();
        testData.put("TEST_ID", testId);
        testData.put("SCORE", score);
        testData.put("MAX_SCORE", maxScore);
        testData.put("PERCENTAGE", percentage);
        testData.put("COMPLETED_AT", System.currentTimeMillis());
        
        progressData.put("TEST_" + testId, testData);
        
        progressDoc.set(progressData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Test completion saved to Firebase: " + testId + " - " + percentage + "%");
                updateCategoryProgress(categoryId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to save test completion: " + e.getMessage());
            });
    }
    
    /**
     * Get test completion percentage for a specific test
     */
    public int getTestCompletion(String categoryId, String testId) {
        String key = KEY_TEST_COMPLETION + categoryId + "_" + testId;
        return prefs.getInt(key, 0);
    }
    
    /**
     * Get overall category completion percentage
     */
    public int getCategoryCompletion(String categoryId) {
        // This would be calculated from all tests in the category
        // For now, return a placeholder value
        return prefs.getInt(KEY_CATEGORY_PROGRESS + categoryId, 0);
    }
    
    /**
     * Update category progress after test completion
     */
    private void updateCategoryProgress(String categoryId) {
        // Calculate overall progress for the category
        // This would involve summing up all test completions
        // For now, we'll implement a simplified version
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;
        
        DocumentReference userDoc = firestore.collection("USERS").document(userId);
        DocumentReference progressDoc = userDoc.collection("PROGRESS").document(categoryId);
        
        progressDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> data = documentSnapshot.getData();
                if (data != null) {
                    int totalTests = 0;
                    int totalCompletion = 0;
                    
                    // Calculate total completion from all tests
                    for (String key : data.keySet()) {
                        if (key.startsWith("TEST_")) {
                            totalTests++;
                            Map<String, Object> testData = (Map<String, Object>) data.get(key);
                            if (testData != null && testData.containsKey("PERCENTAGE")) {
                                totalCompletion += (Long) testData.get("PERCENTAGE");
                            }
                        }
                    }
                    
                    if (totalTests > 0) {
                        int overallCompletion = totalCompletion / totalTests;
                        
                        // Save to local preferences
                        prefs.edit().putInt(KEY_CATEGORY_PROGRESS + categoryId, overallCompletion).apply();
                        
                        // Update Firebase
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("OVERALL_COMPLETION", overallCompletion);
                        updateData.put("TOTAL_TESTS", totalTests);
                        updateData.put("LAST_UPDATED", System.currentTimeMillis());
                        
                        progressDoc.update(updateData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Category progress updated: " + overallCompletion + "%");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update category progress: " + e.getMessage());
                            });
                    }
                }
            }
        });
    }
    
    /**
     * Check if a test should be unlocked based on user progress
     */
    public boolean shouldUnlockTest(String categoryId, String testId, String difficulty, int requiredScore) {
        // TestA is always unlocked (EASY difficulty)
        if (testId.equals("A") || difficulty.equals("EASY")) {
            return true;
        }
        
        // TestB is unlocked only if TestA is completed with at least 75% success rate
        if (testId.equals("B") || difficulty.equals("MEDIUM")) {
            int testACompletion = getTestCompletion(categoryId, "A");
            return testACompletion >= 75;
        }
        
        // TestC is unlocked only if TestB is unlocked (which requires TestA to be completed with 75%)
        if (testId.equals("C") || difficulty.equals("HARD")) {
            int testACompletion = getTestCompletion(categoryId, "A");
            return testACompletion >= 75; // If TestA is completed with 75%, then TestB and TestC are unlocked
        }
        
        // Get category completion percentage (fallback for other tests)
        int categoryCompletion = getCategoryCompletion(categoryId);
        
        // Default rules for other tests based on difficulty
        if (difficulty.equals("MEDIUM")) {
            return categoryCompletion >= 70;
        } else if (difficulty.equals("HARD")) {
            return categoryCompletion >= 85;
        }
        
        return false;
    }
    
    /**
     * Update test unlock status for all tests in a category
     */
    public void updateTestUnlockStatus(String categoryId, List<TestModel> testList) {
        for (TestModel test : testList) {
            boolean shouldUnlock = shouldUnlockTest(
                categoryId, 
                test.getId(), 
                test.getDifficulty(), 
                test.getRequiredScore()
            );
            test.setUnlocked(shouldUnlock);
        }
    }
    
    /**
     * Get user's overall progress across all categories
     */
    public Map<String, Integer> getAllCategoryProgress() {
        Map<String, Integer> progress = new HashMap<>();
        
        // Get all category progress from preferences
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_CATEGORY_PROGRESS)) {
                String categoryId = key.replace(KEY_CATEGORY_PROGRESS, "");
                int completion = prefs.getInt(key, 0);
                progress.put(categoryId, completion);
            }
        }
        
        return progress;
    }
    
    /**
     * Reset progress for testing purposes
     */
    public void resetProgress() {
        prefs.edit().clear().apply();
        Log.d(TAG, "User progress reset");
    }
}
