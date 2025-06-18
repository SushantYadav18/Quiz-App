package com.example.quizpractice;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    private RecyclerView testRecycler;
    private Toolbar toolbar;
    private List<TestModel> testList;
    private TestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);

        try {
            // Initialize views
            toolbar = findViewById(R.id.toolbar);
            testRecycler = findViewById(R.id.testRecycle);
            testList = new ArrayList<>();

            if (testRecycler == null) {
                Log.e(TAG, "RecyclerView is null!");
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_LONG).show();
                return;
            }

            // Setup toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                
                // Get category position and set title
                int position = getIntent().getIntExtra("CAT_INDEX", 0);
                String categoryName = getIntent().getStringExtra("CAT_NAME");
                if (categoryName != null) {
                    getSupportActionBar().setTitle(categoryName);
                }
            }

            // Setup RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            testRecycler.setLayoutManager(layoutManager);
            testRecycler.setHasFixedSize(true);
            testRecycler.setVisibility(View.VISIBLE);

            // Load and display test data
            loadTestData();
            
            // Create and set adapter
            adapter = new TestAdapter(testList, this);
            testRecycler.setAdapter(adapter);
            
            // Force layout update
            testRecycler.requestLayout();
            
            // Log for debugging
            Log.d(TAG, "Number of test items: " + testList.size());
            Log.d(TAG, "RecyclerView visibility: " + testRecycler.getVisibility());
            Log.d(TAG, "RecyclerView height: " + testRecycler.getHeight());
            Log.d(TAG, "RecyclerView width: " + testRecycler.getWidth());
            Log.d(TAG, "Adapter item count: " + adapter.getItemCount());
            
            // Verify data
            for (int i = 0; i < testList.size(); i++) {
                TestModel model = testList.get(i);
                Log.d(TAG, String.format("Test %d: ID=%s, Score=%s", i, model.getId(), model.getTopScore()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadTestData() {
        try {
            testList.clear();
            // Add same test data for all categories with valid scores
            testList.add(new TestModel("1", "80", 10));
            testList.add(new TestModel("2", "65", 15));
            testList.add(new TestModel("3", "90", 20));
            testList.add(new TestModel("4", "45", 25));
            testList.add(new TestModel("5", "75", 30));
            
            // Log for debugging
            Log.d(TAG, "Test data loaded. Size: " + testList.size());
            for (TestModel model : testList) {
                Log.d(TAG, "Test item: " + model.getId() + ", Score: " + model.getTopScore());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading test data: " + e.getMessage());
            e.printStackTrace();
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