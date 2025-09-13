package com.example.quizpractice;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizpractice.UserProgressManager;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    private RecyclerView testRecycler;
    private Toolbar toolbar;
    private TestAdapter adapter;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);

        try {
            Log.d(TAG, "TestActivity onCreate started");
            
            // Initialize loading dialog
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.loading_progressbar);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Initialize views
            toolbar = findViewById(R.id.toolbar);
            testRecycler = findViewById(R.id.testRecycle);

            if (testRecycler == null) {
                Log.e(TAG, "RecyclerView is null!");
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d(TAG, "Views initialized successfully");

            // Setup toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                
                // Get category position and set title
                int catIndex = getIntent().getIntExtra("CAT_INDEX", 0);
                DbQuery.g_selected_cat_index = catIndex;
                
                Log.d(TAG, "Category index received: " + catIndex);
                Log.d(TAG, "Total categories available: " + DbQuery.g_catList.size());
                
                if (catIndex >= 0 && catIndex < DbQuery.g_catList.size()) {
                    String categoryName = DbQuery.g_catList.get(catIndex).getName();
                    getSupportActionBar().setTitle(categoryName);
                    Log.d(TAG, "Loading tests for category: " + categoryName + " (index: " + catIndex + ")");
                } else {
                    Log.e(TAG, "Invalid category index: " + catIndex);
                    Toast.makeText(this, "Error: Invalid category", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }

            // Setup RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            testRecycler.setLayoutManager(layoutManager);
            testRecycler.setHasFixedSize(true);
            testRecycler.setVisibility(View.VISIBLE);

            Log.d(TAG, "RecyclerView setup completed");

            // Show loading dialog
            loadingDialog.show();

            // Load test data from database
            Log.d(TAG, "Starting to load tests...");
            DbQuery.loadTests(new MyCompleteListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Tests loaded successfully. Count: " + DbQuery.g_testList.size());
                    
                    runOnUiThread(() -> {
                        try {
                            // Update test unlock status based on user progress
                            String categoryId = DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getDocID();
                            UserProgressManager progressManager = UserProgressManager.getInstance(TestActivity.this);
                            progressManager.updateTestUnlockStatus(categoryId, DbQuery.g_testList);
                            
                            // Log overall category completion for debugging
                            int categoryCompletion = progressManager.getCategoryCompletion(categoryId);
                            Log.d(TAG, "Overall category completion: " + categoryCompletion + "%");
                            
                            // Initialize adapter with loaded data
                            adapter = new TestAdapter(DbQuery.g_testList, TestActivity.this);
                            testRecycler.setAdapter(adapter);
                            
                            // Log test details for debugging
                            for (int i = 0; i < DbQuery.g_testList.size(); i++) {
                                TestModel test = DbQuery.g_testList.get(i);
                                Log.d(TAG, String.format("Test %d: ID=%s, Time=%d, Difficulty=%s, Unlocked=%s", 
                                    i + 1, test.getId(), test.getTime(), test.getDifficulty(), test.isUnlocked()));
                            }
                            
                            // Dismiss loading dialog
                            loadingDialog.dismiss();
                            
                            Log.d(TAG, "TestActivity setup completed successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting up adapter: " + e.getMessage());
                            e.printStackTrace();
                            loadingDialog.dismiss();
                            Toast.makeText(TestActivity.this, 
                                "Error setting up tests: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "Failed to load tests");
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(TestActivity.this, 
                            "Failed to load tests. Please try again later.", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}