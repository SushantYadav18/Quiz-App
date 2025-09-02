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
    private static final String KEY_BEST_SCORE = "best_score_";
    private static final String KEY_ATTEMPT_COUNT = "attempt_count_";
    
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
        
        // Get current best score and attempt count
        String bestScoreKey = KEY_BEST_SCORE + categoryId + "_" + testId;
        String attemptCountKey = KEY_ATTEMPT_COUNT + categoryId + "_" + testId;
        
        int currentBestScore = prefs.getInt(bestScoreKey, 0);
        int currentAttemptCount = prefs.getInt(attemptCountKey, 0);
        
        // Update best score if this attempt is better
        int newBestScore = Math.max(currentBestScore, percentage);
        int newAttemptCount = currentAttemptCount + 1;
        
        // Save to local preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TEST_COMPLETION + categoryId + "_" + testId, percentage);
        editor.putInt(bestScoreKey, newBestScore);
        editor.putInt(attemptCountKey, newAttemptCount);
        editor.apply();
        
        Log.d(TAG, "Progress saved - Test: " + testId + ", Score: " + percentage + "%, Best: " + newBestScore + "%, Attempts: " + newAttemptCount);
        
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
        testData.put("BEST_SCORE", newBestScore);
        testData.put("ATTEMPT_COUNT", newAttemptCount);
        testData.put("COMPLETED_AT", System.currentTimeMillis());
        
        progressData.put("TEST_" + testId, testData);
        
        progressDoc.set(progressData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Test completion saved to Firebase: " + testId + " - " + percentage + "% (Best: " + newBestScore + "%)");
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
        Log.d(TAG, "Checking unlock for Test " + testId + " (difficulty: " + difficulty + ")");
        
        // EASY tests are always unlocked
        if (difficulty.equals("EASY")) {
            Log.d(TAG, "EASY test - always unlocked");
            return true;
        }
        
        // Calculate category completion percentage from best scores
        int totalEasyTests = 0;
        int totalEasyCompletion = 0;
        
        // Get all test IDs from the current category and check EASY difficulty ones
        // We'll check all possible test IDs to find completed EASY tests
        String[] possibleTestIds = {"A", "B", "C", "1", "2", "3", "AAAA", "BBBB", "CCCC", "DDDD", "EEEE"};
        for (String possibleTestId : possibleTestIds) {
            int bestScore = getBestScore(categoryId, possibleTestId);
            if (bestScore > 0) {
                // Check if this test is EASY difficulty by looking at saved data
                // For now, assume first few tests are EASY
                totalEasyTests++;
                totalEasyCompletion += bestScore;
                Log.d(TAG, "Found completed test " + possibleTestId + ": " + bestScore + "%");
            }
        }
        
        int categoryCompletion = totalEasyTests > 0 ? totalEasyCompletion / totalEasyTests : 0;
        Log.d(TAG, "Category completion: " + categoryCompletion + "% (from " + totalEasyTests + " easy tests)");
        
        // Update local category progress
        prefs.edit().putInt(KEY_CATEGORY_PROGRESS + categoryId, categoryCompletion).apply();
        
        // MEDIUM tests unlock at 70% category completion
        if (difficulty.equals("MEDIUM")) {
            boolean unlocked = categoryCompletion >= 70;
            Log.d(TAG, "MEDIUM test unlock check: " + categoryCompletion + "% >= 70% = " + unlocked);
            return unlocked;
        }
        
        // HARD tests unlock at 85% category completion
        if (difficulty.equals("HARD")) {
            boolean unlocked = categoryCompletion >= 85;
            Log.d(TAG, "HARD test unlock check: " + categoryCompletion + "% >= 85% = " + unlocked);
            return unlocked;
        }
        
        // Default fallback
        Log.d(TAG, "Unknown difficulty - defaulting to unlocked");
        return true;
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
    
    /**
     * Get best score for a specific test
     */
    public int getBestScore(String categoryId, String testId) {
        String key = KEY_BEST_SCORE + categoryId + "_" + testId;
        return prefs.getInt(key, 0);
    }
    
    /**
     * Get attempt count for a specific test
     */
    public int getAttemptCount(String categoryId, String testId) {
        String key = KEY_ATTEMPT_COUNT + categoryId + "_" + testId;
        return prefs.getInt(key, 0);
    }
    
    /**
     * Get comprehensive test progress data for UI display
     */
    public Map<String, Object> getTestProgressData(String categoryId, String testId) {
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("best_score", getBestScore(categoryId, testId));
        progressData.put("attempt_count", getAttemptCount(categoryId, testId));
        progressData.put("is_completed", getBestScore(categoryId, testId) > 0);
        return progressData;
    }
    
    /**
     * Get comprehensive category progress data for UI display
     */
    public Map<String, Object> getCategoryProgressData(String categoryId) {
        Map<String, Object> categoryData = new HashMap<>();
        int overallCompletion = getCategoryCompletion(categoryId);
        categoryData.put("overall_completion", overallCompletion);
        categoryData.put("category_id", categoryId);
        return categoryData;
    }
}
