package com.example.quizpractice;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestManagementActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView testsRecyclerView;
    private TestAdminAdapter testAdapter;
    private List<TestAdminModel> testList;
    private List<CategoryModel> categoryList;
    private Spinner categorySpinner;
    private TextInputEditText testNameInput, testTimeInput;
    private TextInputLayout testNameLayout, testTimeLayout;
    private MaterialButton addTestBtn;
    private ProgressBar progressBar;
    private String selectedCategoryId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_management);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Test Management");

        // Initialize UI elements
        testsRecyclerView = findViewById(R.id.testsRecyclerView);
        categorySpinner = findViewById(R.id.categorySpinner);
        testNameInput = findViewById(R.id.testNameInput);
        testTimeInput = findViewById(R.id.testTimeInput);
        testNameLayout = findViewById(R.id.testNameLayout);
        testTimeLayout = findViewById(R.id.testTimeLayout);
        addTestBtn = findViewById(R.id.addTestBtn);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        testList = new ArrayList<>();
        testAdapter = new TestAdminAdapter(testList);
        testsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        testsRecyclerView.setAdapter(testAdapter);

        // Set click listeners
        testAdapter.setOnEditClickListener(this::showEditTestDialog);
        testAdapter.setOnDeleteClickListener(this::showDeleteTestDialog);
        testAdapter.setOnManageQuestionsClickListener(this::navigateToQuestionManagement);

        addTestBtn.setOnClickListener(v -> addNewTest());

        // Load categories for spinner
        loadCategories();

        // Setup spinner listener
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && categoryList != null && position <= categoryList.size()) {
                    selectedCategoryId = categoryList.get(position - 1).getDocID();
                    loadTests(selectedCategoryId);
                } else {
                    selectedCategoryId = "";
                    testList.clear();
                    testAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategoryId = "";
            }
        });
    }

    private void loadCategories() {
        showProgressBar();
        categoryList = new ArrayList<>();

        db.collection("QUIZ").document("Categories").collection("CAT")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("NAME");
                        int noOfTests = 0;
                        if (document.contains("NO_OF_TESTS")) {
                            Object noOfTestsObj = document.get("NO_OF_TESTS");
                            if (noOfTestsObj instanceof Long) {
                                noOfTests = ((Long) noOfTestsObj).intValue();
                            } else if (noOfTestsObj instanceof String) {
                                try {
                                    noOfTests = Integer.parseInt((String) noOfTestsObj);
                                } catch (NumberFormatException e) {
                                    noOfTests = 0;
                                }
                            }
                        }
                        categoryList.add(new CategoryModel(id, name, noOfTests));
                    }

                    setupCategorySpinner();
                    hideProgressBar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TestManagementActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Select Category");

        for (CategoryModel category : categoryList) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void loadTests(String categoryId) {
        if (categoryId.isEmpty()) {
            return;
        }

        showProgressBar();
        testList.clear();

        db.collection("QUIZ").document("Categories").collection("CAT")
                .document(categoryId).collection("TESTS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("NAME");
                        int time = 0;
                        if (document.contains("TIME")) {
                            Object timeObj = document.get("TIME");
                            if (timeObj instanceof Long) {
                                time = ((Long) timeObj).intValue();
                            } else if (timeObj instanceof String) {
                                try {
                                    time = Integer.parseInt((String) timeObj);
                                } catch (NumberFormatException e) {
                                    time = 0;
                                }
                            }
                        }

                        // Get question count
                        int questionCount = 0;
                        if (document.contains("QUESTION_COUNT")) {
                            Object countObj = document.get("QUESTION_COUNT");
                            if (countObj instanceof Long) {
                                questionCount = ((Long) countObj).intValue();
                            } else if (countObj instanceof String) {
                                try {
                                    questionCount = Integer.parseInt((String) countObj);
                                } catch (NumberFormatException e) {
                                    questionCount = 0;
                                }
                            }
                        }

                        testList.add(new TestAdminModel(id, name, time, questionCount, categoryId));
                    }

                    testAdapter.notifyDataSetChanged();
                    hideProgressBar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TestManagementActivity.this, "Error loading tests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
    }

    private void addNewTest() {
        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Please select a category first", Toast.LENGTH_SHORT).show();
            return;
        }

        String testName = testNameInput.getText().toString().trim();
        String testTimeStr = testTimeInput.getText().toString().trim();

        if (testName.isEmpty()) {
            testNameLayout.setError("Test name cannot be empty");
            return;
        } else {
            testNameLayout.setError(null);
        }

        if (testTimeStr.isEmpty()) {
            testTimeLayout.setError("Time cannot be empty");
            return;
        } else {
            testTimeLayout.setError(null);
        }

        int testTime;
        try {
            testTime = Integer.parseInt(testTimeStr);
            if (testTime <= 0) {
                testTimeLayout.setError("Time must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            testTimeLayout.setError("Invalid time format");
            return;
        }

        showProgressBar();

        // Create a new test document
        Map<String, Object> testData = new HashMap<>();
        testData.put("NAME", testName);
        testData.put("TIME", testTime);
        testData.put("QUESTION_COUNT", 0);

        db.collection("QUIZ").document("Categories").collection("CAT")
                .document(selectedCategoryId).collection("TESTS")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    String testId = documentReference.getId();
                    TestAdminModel newTest = new TestAdminModel(testId, testName, testTime, 0, selectedCategoryId);
                    testList.add(newTest);
                    testAdapter.notifyDataSetChanged();

                    // Clear input fields
                    testNameInput.setText("");
                    testTimeInput.setText("");

                    Toast.makeText(TestManagementActivity.this, "Test added successfully", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TestManagementActivity.this, "Error adding test: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
    }

    private void showEditTestDialog(TestAdminModel test) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_test, null);
        builder.setView(dialogView);

        TextInputEditText nameInput = dialogView.findViewById(R.id.editTestNameInput);
        TextInputEditText timeInput = dialogView.findViewById(R.id.editTestTimeInput);

        nameInput.setText(test.getName());
        timeInput.setText(String.valueOf(test.getTime()));

        builder.setTitle("Edit Test");
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newTimeStr = timeInput.getText().toString().trim();

            if (newName.isEmpty() || newTimeStr.isEmpty()) {
                Toast.makeText(TestManagementActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            int newTime;
            try {
                newTime = Integer.parseInt(newTimeStr);
                if (newTime <= 0) {
                    Toast.makeText(TestManagementActivity.this, "Time must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(TestManagementActivity.this, "Invalid time format", Toast.LENGTH_SHORT).show();
                return;
            }

            updateTest(test.getDocID(), test.getCategoryID(), newName, newTime);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateTest(String testId, String categoryId, String newName, int newTime) {
        showProgressBar();

        Map<String, Object> updates = new HashMap<>();
        updates.put("NAME", newName);
        updates.put("TIME", newTime);

        db.collection("QUIZ").document("Categories").collection("CAT")
                .document(categoryId).collection("TESTS").document(testId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local list
                    for (int i = 0; i < testList.size(); i++) {
                        if (testList.get(i).getDocID().equals(testId)) {
                            testList.get(i).setName(newName);
                            testList.get(i).setTime(newTime);
                            testAdapter.notifyItemChanged(i);
                            break;
                        }
                    }

                    Toast.makeText(TestManagementActivity.this, "Test updated successfully", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TestManagementActivity.this, "Error updating test: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
    }

    private void showDeleteTestDialog(TestAdminModel test) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Test")
                .setMessage("Are you sure you want to delete this test? This will also delete all questions associated with this test.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTest(test.getDocID(), test.getCategoryID()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTest(String testId, String categoryId) {
        showProgressBar();

        // First, delete all questions in this test
        db.collection("QUIZ").document("Categories").collection("CAT")
                .document(categoryId).collection("TESTS").document(testId)
                .collection("QUESTIONS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Batch delete all questions
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // Then delete the test itself
                    db.collection("QUIZ").document("Categories").collection("CAT")
                            .document(categoryId).collection("TESTS").document(testId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from local list
                                for (int i = 0; i < testList.size(); i++) {
                                    if (testList.get(i).getDocID().equals(testId)) {
                                        testList.remove(i);
                                        testAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }

                                Toast.makeText(TestManagementActivity.this, "Test deleted successfully", Toast.LENGTH_SHORT).show();
                                hideProgressBar();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TestManagementActivity.this, "Error deleting test: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                hideProgressBar();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TestManagementActivity.this, "Error deleting questions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void navigateToQuestionManagement(TestAdminModel test) {
        android.content.Intent intent = new android.content.Intent(this, QuestionManagementActivity.class);
        intent.putExtra("TEST_ID", test.getDocID());
        intent.putExtra("TEST_NAME", test.getName());
        intent.putExtra("CATEGORY_ID", test.getCategoryID());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}