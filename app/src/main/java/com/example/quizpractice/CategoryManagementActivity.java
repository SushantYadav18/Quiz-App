package com.example.quizpractice;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManagementActivity extends AppCompatActivity {
    private static final String TAG = "CategoryManagement";
    
    private RecyclerView categoriesRecyclerView;
    private TextInputEditText categoryNameInput;
    private MaterialButton addCategoryBtn;
    private ProgressBar progressBar;
    
    private FirebaseFirestore firestore;
    private CategoryAdapter categoryAdapter;
    private List<CategoryModel> categoryList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);
        
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Category Management");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Initialize views
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        categoryNameInput = findViewById(R.id.categoryNameInput);
        addCategoryBtn = findViewById(R.id.addCategoryBtn);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup RecyclerView
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(categoryAdapter);
        
        // Set click listeners
        addCategoryBtn.setOnClickListener(v -> addCategory());
        
        // Set adapter callbacks
        categoryAdapter.setOnEditClickListener(this::showEditCategoryDialog);
        categoryAdapter.setOnDeleteClickListener(this::showDeleteCategoryDialog);
        
        // Load categories
        loadCategories();
    }
    
    private void loadCategories() {
        setLoading(true);
        
        firestore.collection("QUIZ").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        CategoryModel category = new CategoryModel(
                                doc.getId(),
                                doc.getString("NAME"),
                                Integer.parseInt(doc.getString("NO_OF_TESTS") != null ? doc.getString("NO_OF_TESTS") : "0")
                        );
                        categoryList.add(category);
                    }
                    
                    categoryAdapter.notifyDataSetChanged();
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Failed to load categories", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }
    
    private void addCategory() {
        String categoryName = categoryNameInput.getText().toString().trim();
        
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        
        // Generate a new document ID
        String categoryId = firestore.collection("QUIZ").document().getId();
        
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("NAME", categoryName);
        categoryData.put("NO_OF_TESTS", "0");
        
        firestore.collection("QUIZ").document(categoryId)
                .set(categoryData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Category added successfully", Toast.LENGTH_SHORT).show();
                    categoryNameInput.setText("");
                    loadCategories(); // Reload categories
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding category", e);
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Failed to add category", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }
    
    private void showEditCategoryDialog(CategoryModel category, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Category");
        
        // Set up the input
        final TextInputEditText input = new TextInputEditText(this);
        input.setText(category.getName());
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateCategory(category.getDocID(), newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void updateCategory(String categoryId, String newName) {
        setLoading(true);
        
        firestore.collection("QUIZ").document(categoryId)
                .update("NAME", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Category updated successfully", Toast.LENGTH_SHORT).show();
                    loadCategories(); // Reload categories
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating category", e);
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Failed to update category", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }
    
    private void showDeleteCategoryDialog(CategoryModel category, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete this category? All tests and questions within this category will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCategory(category.getDocID()))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteCategory(String categoryId) {
        setLoading(true);
        
        // First, delete all tests in this category
        firestore.collection("QUIZ").document(categoryId).collection("TESTS")
                .get()
                .addOnSuccessListener(testSnapshots -> {
                    // For each test, delete all questions
                    for (DocumentSnapshot testDoc : testSnapshots) {
                        String testId = testDoc.getId();
                        
                        // Delete questions subcollection (this is a batch operation in a real app)
                        firestore.collection("QUIZ").document(categoryId)
                                .collection("TESTS").document(testId)
                                .collection("QUESTIONS")
                                .get()
                                .addOnSuccessListener(questionSnapshots -> {
                                    for (DocumentSnapshot questionDoc : questionSnapshots) {
                                        questionDoc.getReference().delete();
                                    }
                                });
                        
                        // Delete the test document
                        testDoc.getReference().delete();
                    }
                    
                    // Finally delete the category document
                    firestore.collection("QUIZ").document(categoryId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CategoryManagementActivity.this, 
                                        "Category deleted successfully", Toast.LENGTH_SHORT).show();
                                loadCategories(); // Reload categories
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting category", e);
                                Toast.makeText(CategoryManagementActivity.this, 
                                        "Failed to delete category", Toast.LENGTH_SHORT).show();
                                setLoading(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting tests in category", e);
                    Toast.makeText(CategoryManagementActivity.this, 
                            "Failed to delete category", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }
    
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
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