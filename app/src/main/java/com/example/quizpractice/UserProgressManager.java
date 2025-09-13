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
    private String currentUserId;
    
    private UserProgressManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        
        // Get current user ID if available
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            this.currentUserId = auth.getCurrentUser().getUid();
            this.prefs = getUserSpecificSharedPreferences();
        } else {
            // Default to non-user specific prefs if no user is logged in
            // This should be temporary until a user logs in
            this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
    
    public static synchronized UserProgressManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserProgressManager(context);
        } else {
            // Check if user has changed since last getInstance call
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                if (!userId.equals(instance.currentUserId)) {
                    // User has changed, create new instance with updated user
                    instance = new UserProgressManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get user-specific SharedPreferences
     */
    private SharedPreferences getUserSpecificSharedPreferences() {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            // Use user ID in preferences name to keep data separate per user
            return context.getSharedPreferences(PREF_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
        } else {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Update the current user ID and refresh SharedPreferences
     */
    public void setCurrentUser(String userId) {
        if (userId != null && !userId.equals(currentUserId)) {
            this.currentUserId = userId;
            this.prefs = getUserSpecificSharedPreferences();
            Log.d(TAG, "User changed to: " + userId + ", preferences updated");
        }
    }
    
    /**
     * Save test completion result
     */
    public void saveTestCompletion(String categoryId, String testId, int score, int maxScore) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        
        // Ensure we're using the correct user's preferences
        if (!userId.equals(currentUserId)) {
            setCurrentUser(userId);
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
        
        // For MEDIUM tests: Check if ALL easy tests are completed with at least 70% score
        if (difficulty.equals("MEDIUM")) {
            // Get all EASY tests from the category
            int easyTestsCount = 0;
            int easyTestsCompleted = 0;
            int totalEasyScore = 0;
            
            // First, find all EASY tests in the category by checking DbQuery.g_testList
            // This is more accurate than guessing based on test IDs
            List<TestModel> easyTests = new java.util.ArrayList<>();
            
            // Check if we have access to the global test list
            if (DbQuery.g_testList != null && !DbQuery.g_testList.isEmpty()) {
                // Find all EASY tests in the current category
                for (TestModel test : DbQuery.g_testList) {
                    if (test.getDifficulty().equals("EASY")) {
                        easyTests.add(test);
                        easyTestsCount++;
                        
                        // Get the best score for this test
                        int bestScore = getBestScore(categoryId, test.getId());
                        totalEasyScore += bestScore;
                        
                        if (bestScore >= 70) {
                            easyTestsCompleted++;
                            Log.d(TAG, "Found completed EASY test " + test.getId() + ": " + bestScore + "%");
                        } else {
                            Log.d(TAG, "Found incomplete EASY test " + test.getId() + ": " + bestScore + "% (below 70%)");
                        }
                    }
                }
            } else {
                // Fallback to the old method if g_testList is not available
                // Get all test IDs from the current category and check EASY difficulty ones
                String[] possibleTestIds = {"A", "B", "C", "1", "2", "3", "AAAA", "BBBB", "CCCC", "DDDD", "EEEE"};
                for (String possibleTestId : possibleTestIds) {
                    // Check if this is an EASY test by looking at saved data
                    boolean isEasyTest = false;
                    
                    // Check if we have any data about this test
                    int bestScore = getBestScore(categoryId, possibleTestId);
                    if (bestScore > 0) {
                        // This is a test the user has attempted
                        // For simplicity, we'll consider tests with IDs A, 1, AAAA as EASY
                        if (possibleTestId.equals("A") || possibleTestId.equals("1") || 
                            possibleTestId.equals("AAAA") || possibleTestId.equals("BBBB")) {
                            isEasyTest = true;
                        }
                        
                        if (isEasyTest) {
                            easyTestsCount++;
                            totalEasyScore += bestScore;
                            
                            if (bestScore >= 70) {
                                easyTestsCompleted++;
                                Log.d(TAG, "Found completed EASY test " + possibleTestId + ": " + bestScore + "%");
                            } else {
                                Log.d(TAG, "Found incomplete EASY test " + possibleTestId + ": " + bestScore + "% (below 70%)");
                            }
                        }
                    }
                }
            }
            
            // Calculate the overall completion percentage for all EASY tests
            int overallEasyCompletion = (easyTestsCount > 0) ? (totalEasyScore / easyTestsCount) : 0;
            Log.d(TAG, "Overall EASY tests completion: " + overallEasyCompletion + "%");
            
            // Check if ALL easy tests are completed with at least 70% score individually
            boolean allEasyTestsCompleted = (easyTestsCompleted == easyTestsCount && easyTestsCount > 0);
            
            // Check if the overall completion of ALL easy tests is at least 70%
            boolean overallEasyCompletionSufficient = (overallEasyCompletion >= 70);
            
            Log.d(TAG, "MEDIUM test unlock check: " + easyTestsCompleted + "/" + easyTestsCount + 
                  " EASY tests completed at 70%+ = " + allEasyTestsCompleted);
            Log.d(TAG, "Overall EASY completion: " + overallEasyCompletion + "% (needs 70%+) = " + overallEasyCompletionSufficient);
            
            // We need BOTH conditions to be true: all individual tests at 70%+ AND overall completion at 70%+
            return allEasyTestsCompleted && overallEasyCompletionSufficient;
        }
        
        // For HARD tests: Check if ALL medium tests are completed with at least 70% score
        if (difficulty.equals("HARD")) {
            // Get all MEDIUM tests from the category
            int mediumTestsCount = 0;
            int mediumTestsCompleted = 0;
            int totalMediumScore = 0;
            
            // First, find all MEDIUM tests in the category by checking DbQuery.g_testList
            // This is more accurate than guessing based on test IDs
            List<TestModel> mediumTests = new java.util.ArrayList<>();
            
            // Check if we have access to the global test list
            if (DbQuery.g_testList != null && !DbQuery.g_testList.isEmpty()) {
                // Find all MEDIUM tests in the current category
                for (TestModel test : DbQuery.g_testList) {
                    if (test.getDifficulty().equals("MEDIUM")) {
                        mediumTests.add(test);
                        mediumTestsCount++;
                        
                        // Get the best score for this test
                        int bestScore = getBestScore(categoryId, test.getId());
                        totalMediumScore += bestScore;
                        
                        if (bestScore >= 70) {
                            mediumTestsCompleted++;
                            Log.d(TAG, "Found completed MEDIUM test " + test.getId() + ": " + bestScore + "%");
                        } else {
                            Log.d(TAG, "Found incomplete MEDIUM test " + test.getId() + ": " + bestScore + "% (below 70%)");
                        }
                    }
                }
            } else {
                // Fallback to the old method if g_testList is not available
                // Get all test IDs from the current category and check MEDIUM difficulty ones
                String[] possibleTestIds = {"A", "B", "C", "1", "2", "3", "AAAA", "BBBB", "CCCC", "DDDD", "EEEE"};
                for (String possibleTestId : possibleTestIds) {
                    // Check if this is a MEDIUM test
                    boolean isMediumTest = false;
                    
                    // Check if we have any data about this test
                    int bestScore = getBestScore(categoryId, possibleTestId);
                    if (bestScore > 0) {
                        // This is a test the user has attempted
                        // For simplicity, we'll consider tests with IDs B, 2, CCCC as MEDIUM
                        if (possibleTestId.equals("B") || possibleTestId.equals("2") || 
                            possibleTestId.equals("CCCC") || possibleTestId.equals("DDDD")) {
                            isMediumTest = true;
                        }
                        
                        if (isMediumTest) {
                            mediumTestsCount++;
                            totalMediumScore += bestScore;
                            
                            if (bestScore >= 70) {
                                mediumTestsCompleted++;
                                Log.d(TAG, "Found completed MEDIUM test " + possibleTestId + ": " + bestScore + "%");
                            } else {
                                Log.d(TAG, "Found incomplete MEDIUM test " + possibleTestId + ": " + bestScore + "% (below 70%)");
                            }
                        }
                    }
                }
            }
            
            // Calculate the overall completion percentage for all MEDIUM tests
            int overallMediumCompletion = (mediumTestsCount > 0) ? (totalMediumScore / mediumTestsCount) : 0;
            Log.d(TAG, "Overall MEDIUM tests completion: " + overallMediumCompletion + "%");
            
            // Check if ALL medium tests are completed with at least 70% score individually
            boolean allMediumTestsCompleted = (mediumTestsCompleted == mediumTestsCount && mediumTestsCount > 0);
            
            // Check if the overall completion of ALL medium tests is at least 70%
            boolean overallMediumCompletionSufficient = (overallMediumCompletion >= 70);
            
            Log.d(TAG, "HARD test unlock check: " + mediumTestsCompleted + "/" + mediumTestsCount + 
                  " MEDIUM tests completed at 70%+ = " + allMediumTestsCompleted);
            Log.d(TAG, "Overall MEDIUM completion: " + overallMediumCompletion + "% (needs 70%+) = " + overallMediumCompletionSufficient);
            
            // We need BOTH conditions to be true: all individual tests at 70%+ AND overall completion at 70%+
            return allMediumTestsCompleted && overallMediumCompletionSufficient;
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
        Log.d(TAG, "User progress reset for user: " + currentUserId);
    }
    
    /**
     * Clear local progress data when user logs out
     */
    public void clearUserData() {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            // Clear the current user's preferences
            prefs.edit().clear().apply();
            Log.d(TAG, "User progress data cleared for user: " + currentUserId);
            
            // Reset current user ID
            currentUserId = null;
            
            // Reset to default preferences
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Load user progress from Firebase into SharedPreferences
     */
    public void loadUserProgressFromFirebase(final MyCompleteListener completeListener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Cannot load progress: User not authenticated");
            if (completeListener != null) {
                completeListener.onFailure();
            }
            return;
        }
        
        final String userId = auth.getCurrentUser().getUid();
        
        // Ensure we're using the correct user's preferences
        if (!userId.equals(currentUserId)) {
            setCurrentUser(userId);
        }
        
        // Clear existing local data before loading from Firebase
        resetProgress();
        
        Log.d(TAG, "Loading user progress from Firebase for user: " + userId);
        
        // Load progress data for all categories
        firestore.collection("USERS").document(userId)
            .collection("PROGRESS").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No progress data found for user: " + userId);
                    if (completeListener != null) {
                        completeListener.onSuccess();
                    }
                    return;
                }
                
                Log.d(TAG, "Loading progress data for user: " + userId);
                SharedPreferences.Editor editor = prefs.edit();
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    String categoryId = document.getId();
                    Map<String, Object> data = document.getData();
                    
                    if (data != null) {
                        // Process category overall completion
                        if (data.containsKey("OVERALL_COMPLETION")) {
                            Long overallCompletion = (Long) data.get("OVERALL_COMPLETION");
                            if (overallCompletion != null) {
                                editor.putInt(KEY_CATEGORY_PROGRESS + categoryId, overallCompletion.intValue());
                                Log.d(TAG, "Category " + categoryId + " overall completion: " + overallCompletion + "%");
                            }
                        }
                        
                        // Process individual test data
                        for (String key : data.keySet()) {
                            if (key.startsWith("TEST_")) {
                                String testId = key.replace("TEST_", "");
                                Map<String, Object> testData = (Map<String, Object>) data.get(key);
                                
                                if (testData != null) {
                                    // Save test completion percentage
                                    Long percentage = (Long) testData.get("PERCENTAGE");
                                    Long bestScore = (Long) testData.get("BEST_SCORE");
                                    Long attemptCount = (Long) testData.get("ATTEMPT_COUNT");
                                    
                                    if (percentage != null) {
                                        editor.putInt(KEY_TEST_COMPLETION + categoryId + "_" + testId, percentage.intValue());
                                    }
                                    
                                    // Save best score
                                    if (bestScore != null) {
                                        editor.putInt(KEY_BEST_SCORE + categoryId + "_" + testId, bestScore.intValue());
                                    }
                                    
                                    // Save attempt count
                                    if (attemptCount != null) {
                                        editor.putInt(KEY_ATTEMPT_COUNT + categoryId + "_" + testId, attemptCount.intValue());
                                    }
                                    
                                    Log.d(TAG, "Loaded test " + testId + " in category " + categoryId + 
                                          ": Score=" + percentage + "%, Best=" + bestScore + "%, Attempts=" + attemptCount);
                                }
                            }
                        }
                    }
                }
                
                editor.apply();
                Log.d(TAG, "User progress data loaded from Firebase for user: " + userId);
                
                if (completeListener != null) {
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load user progress data: " + e.getMessage());
                if (completeListener != null) {
                    completeListener.onFailure();
                }
            });
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
