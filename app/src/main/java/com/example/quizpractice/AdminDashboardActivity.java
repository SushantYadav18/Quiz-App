package com.example.quizpractice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";
    private TextView totalUsersText, activeUsersText, totalTestsText, totalQuestionsText, totalAttemptsText;
    private MaterialButton manageCategoriesBtn, manageTestsBtn, manageQuestionsBtn, logoutBtn;
    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        
        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this);
        
        // Check if user is logged in as admin
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Admin Dashboard");
        
        // Initialize views
        totalUsersText = findViewById(R.id.totalUsersText);
        activeUsersText = findViewById(R.id.activeUsersText);
        totalTestsText = findViewById(R.id.totalTestsText);
        totalQuestionsText = findViewById(R.id.totalQuestionsText);
        totalAttemptsText = findViewById(R.id.totalAttemptsText);
        manageCategoriesBtn = findViewById(R.id.manageCategoriesBtn);
        manageTestsBtn = findViewById(R.id.manageTestsBtn);
        manageQuestionsBtn = findViewById(R.id.manageQuestionsBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        
        // Set click listeners
        manageCategoriesBtn.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, CategoryManagementActivity.class));
        });
        
        manageTestsBtn.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, TestManagementActivity.class));
        });
        
        manageQuestionsBtn.setOnClickListener(v -> {
            // Navigate to test management first, as questions are managed per test
            startActivity(new Intent(AdminDashboardActivity.this, TestManagementActivity.class));
        });
        
        logoutBtn.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
            finish();
        });
        
        // Load user statistics
        loadUserStatistics();
    }
    
    private void loadUserStatistics() {
        // Query Firestore to get user statistics
        firestore.collection("USERS").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = 0;
                    int activeUsers = 0;
                    
                    // Count actual users (exclude TOTAL_USERS document)
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!"TOTAL_USERS".equals(doc.getId())) {
                            totalUsers++;
                            // Consider a user active if they have a TOTAL_SCORE > 0
                            Long score = doc.getLong("TOTAL_SCORE");
                            if (score != null && score > 0) {
                                activeUsers++;
                            }
                        }
                    }
                    
                    totalUsersText.setText("Total Users: " + totalUsers);
                    activeUsersText.setText("Active Users: " + activeUsers);
                    
                    // Load additional statistics
                    loadTestStatistics();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user statistics", e);
                    Toast.makeText(AdminDashboardActivity.this, 
                            "Failed to load user statistics", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadTestStatistics() {
        // Get total tests count
        firestore.collection("CATEGORIES").get()
                .addOnSuccessListener(categorySnapshots -> {
                    final AtomicInteger totalTests = new AtomicInteger(0);
                    int totalQuestions = 0;
                    Map<String, Integer> categoryTestCounts = new HashMap<>();
                    
                    // For each category, count tests
                    for (QueryDocumentSnapshot categoryDoc : categorySnapshots) {
                        String categoryId = categoryDoc.getId();
                        
                        // Get test count for this category
                        firestore.collection("CATEGORIES")
                                .document(categoryId)
                                .collection("TESTS_INFO")
                                .document("TESTS_INFO")
                                .get()
                                .addOnSuccessListener(testInfoDoc -> {
                                    if (testInfoDoc.exists()) {
                                        Long testCount = testInfoDoc.getLong("TEST_COUNT");
                                        if (testCount != null) {
                                            totalTests.addAndGet(testCount.intValue());
                                            categoryTestCounts.put(categoryId, testCount.intValue());
                                            totalTestsText.setText("Total Tests: " + totalTests.get());
                                            
                                            // Now count questions for each test in this category
                                            countQuestionsForCategory(categoryId, categoryTestCounts.get(categoryId));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading test info for category: " + categoryId, e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    Toast.makeText(AdminDashboardActivity.this, 
                            "Failed to load test statistics", Toast.LENGTH_SHORT).show();
                });
                
        // Count total test attempts
        countTotalTestAttempts();
    }
    
    private void countQuestionsForCategory(String categoryId, int testCount) {
        // For each test in the category, count questions
        for (int i = 1; i <= testCount; i++) {
            final String testId = "TEST_" + i;
            
            firestore.collection("Questions")
                    .whereEqualTo("CATEGORY", categoryId)
                    .whereEqualTo("TEST", testId)
                    .get()
                    .addOnSuccessListener(questionSnapshots -> {
                        int questionCount = questionSnapshots.size();
                        // Update the total questions count
                        int currentCount = 0;
                        if (!totalQuestionsText.getText().toString().equals("Total Questions: 0")) {
                            String currentText = totalQuestionsText.getText().toString();
                            currentCount = Integer.parseInt(currentText.substring(currentText.lastIndexOf(" ") + 1));
                        }
                        totalQuestionsText.setText("Total Questions: " + (currentCount + questionCount));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error counting questions for test: " + testId, e);
                    });
        }
    }
    
    private void countTotalTestAttempts() {
        // Count all test attempts across all users
        firestore.collection("USERS").get()
                .addOnSuccessListener(userSnapshots -> {
                    final int[] totalAttempts = {0};
                    final int[] usersProcessed = {0};
                    final int totalUsers = userSnapshots.size();
                    
                    if (totalUsers == 0) {
                        totalAttemptsText.setText("Total Test Attempts: 0");
                        return;
                    }
                    
                    for (QueryDocumentSnapshot userDoc : userSnapshots) {
                        if ("TOTAL_USERS".equals(userDoc.getId())) {
                            usersProcessed[0]++;
                            continue;
                        }
                        
                        // For each user, count test results
                        userDoc.getReference().collection("TEST_RESULTS").get()
                                .addOnSuccessListener(testResultSnapshots -> {
                                    for (QueryDocumentSnapshot resultDoc : testResultSnapshots) {
                                        Long attemptCount = resultDoc.getLong("ATTEMPT_COUNT");
                                        if (attemptCount != null) {
                                            totalAttempts[0] += attemptCount;
                                        } else {
                                            // If ATTEMPT_COUNT doesn't exist, count it as 1 attempt
                                            totalAttempts[0]++;
                                        }
                                    }
                                    
                                    usersProcessed[0]++;
                                    if (usersProcessed[0] >= totalUsers) {
                                        // All users processed, update UI
                                        totalAttemptsText.setText("Total Test Attempts: " + totalAttempts[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error counting test attempts for user: " + userDoc.getId(), e);
                                    usersProcessed[0]++;
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users for attempt counting", e);
                    totalAttemptsText.setText("Total Test Attempts: 0");
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}