package com.example.quizpractice;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class QuestionsActivity extends AppCompatActivity {

    private RecyclerView questionView;
    private TextView tvQuesID, timerTV, catNameTV;
    private Button submitB, markB, clearSelB;
    private ImageButton prevQuesB, nextQuesB;
    private ImageView quesListB, bookmarkB;
    
    private QuestionAdapter questionAdapter;
    private int currentQuestionIndex = 0;
    private int totalQuestions = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_questions);

        init();
        setupQuestionData();
        setupClickListeners();
    }

    private void init() {
        questionView = findViewById(R.id.questions_view);

        // Top bar views
        tvQuesID = findViewById(R.id.question_Id);
        timerTV = findViewById(R.id.Tv_timer);
        submitB = findViewById(R.id.submitBtn);

        // Category bar views
        catNameTV = findViewById(R.id.category_name);
        bookmarkB = findViewById(R.id.bookMrk_btn);
        quesListB = findViewById(R.id.grid_view);

        // Bottom bar views
        prevQuesB = findViewById(R.id.nextBtn1);
        clearSelB = findViewById(R.id.clr_sel);
        markB = findViewById(R.id.mark_btn);
        nextQuesB = findViewById(R.id.nextBtn2);
    }

    private void setupQuestionData() {
        // Check if questions are loaded
        if (DbQuery.g_questionList == null || DbQuery.g_questionList.isEmpty()) {
            Log.e("QuestionsActivity", "No questions available. g_questionList is null or empty");
            Toast.makeText(this, "No questions available. Please try again.", Toast.LENGTH_LONG).show();
            
            // Try to load questions again
            loadQuestionsFromDatabase();
            return;
        }

        totalQuestions = DbQuery.g_questionList.size();
        currentQuestionIndex = 0;

        Log.d("QuestionsActivity", "Setting up " + totalQuestions + " questions");

        // Set category name
        if (DbQuery.g_catList != null && !DbQuery.g_catList.isEmpty() && 
            DbQuery.g_selected_cat_index < DbQuery.g_catList.size()) {
            catNameTV.setText(DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getName());
            Log.d("QuestionsActivity", "Category: " + DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getName());
        }

        // Set timer (get from test data)
        if (DbQuery.g_testList != null && !DbQuery.g_testList.isEmpty() && 
            DbQuery.g_selected_test_index < DbQuery.g_testList.size()) {
            int timeInMinutes = DbQuery.g_testList.get(DbQuery.g_selected_test_index).getTime();
            timerTV.setText(String.format("%02d:00 min", timeInMinutes));
            Log.d("QuestionsActivity", "Test time: " + timeInMinutes + " minutes");
        }

        // Setup RecyclerView
        questionAdapter = new QuestionAdapter(DbQuery.g_questionList);
        questionView.setAdapter(questionAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        questionView.setLayoutManager(layoutManager);

        // Update question counter
        updateQuestionCounter();
        
        // Scroll to first question
        questionView.scrollToPosition(0);
        
        Log.d("QuestionsActivity", "Questions setup completed successfully");
    }

    private void loadQuestionsFromDatabase() {
        Log.d("QuestionsActivity", "Attempting to load questions from database");
        
        // Show loading dialog
        android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
            .setView(R.layout.loading_progressbar)
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        // Try to load questions
        DbQuery.loadquestions(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                loadingDialog.dismiss();
                if (DbQuery.g_questionList != null && !DbQuery.g_questionList.isEmpty()) {
                    Log.d("QuestionsActivity", "Questions loaded successfully: " + DbQuery.g_questionList.size());
                    setupQuestionData();
                } else {
                    Log.e("QuestionsActivity", "Questions still not available after loading");
                    Toast.makeText(QuestionsActivity.this, 
                        "Failed to load questions. Please try again.", 
                        Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure() {
                loadingDialog.dismiss();
                Log.e("QuestionsActivity", "Failed to load questions from database");
                Toast.makeText(QuestionsActivity.this, 
                    "Failed to load questions. Please check your connection and try again.", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void setupClickListeners() {
        // Navigation buttons
        prevQuesB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIndex > 0) {
                    currentQuestionIndex--;
                    questionView.smoothScrollToPosition(currentQuestionIndex);
                    updateQuestionCounter();
                }
            }
        });

        nextQuesB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestionIndex < totalQuestions - 1) {
                    currentQuestionIndex++;
                    questionView.smoothScrollToPosition(currentQuestionIndex);
                    updateQuestionCounter();
                }
            }
        });

        // Action buttons
        clearSelB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear current question selection
                if (questionAdapter != null) {
                    questionAdapter.clearSelection(currentQuestionIndex);
                }
                Toast.makeText(QuestionsActivity.this, "Selection cleared", Toast.LENGTH_SHORT).show();
            }
        });

        markB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mark question for review
                if (questionAdapter != null) {
                    questionAdapter.markForReview(currentQuestionIndex);
                }
                Toast.makeText(QuestionsActivity.this, "Question marked for review", Toast.LENGTH_SHORT).show();
            }
        });

        submitB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Submit quiz
                showSubmitConfirmation();
            }
        });

        bookmarkB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bookmark current question
                if (questionAdapter != null) {
                    questionAdapter.toggleBookmark(currentQuestionIndex);
                }
                Toast.makeText(QuestionsActivity.this, "Question bookmarked", Toast.LENGTH_SHORT).show();
            }
        });

        quesListB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show question grid
                showQuestionGrid();
            }
        });

        // Add scroll listener to track current question
        questionView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int position = layoutManager.findFirstVisibleItemPosition();
                        if (position != RecyclerView.NO_POSITION && position != currentQuestionIndex) {
                            currentQuestionIndex = position;
                            updateQuestionCounter();
                        }
                    }
                }
            }
        });
    }

    private void updateQuestionCounter() {
        tvQuesID.setText(String.format("%d/%d", currentQuestionIndex + 1, totalQuestions));
        
        // Update navigation button states
        prevQuesB.setEnabled(currentQuestionIndex > 0);
        nextQuesB.setEnabled(currentQuestionIndex < totalQuestions - 1);
    }

    private void showSubmitConfirmation() {
        // Show confirmation dialog before submitting
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Submit Quiz")
                .setMessage("Are you sure you want to submit your quiz? You cannot change answers after submission.")
                .setPositiveButton("Submit", (dialog, which) -> {
                    submitQuiz();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void submitQuiz() {
        // Calculate score and show results
        if (questionAdapter != null) {
            int score = questionAdapter.calculateScore();
            int totalQuestions = DbQuery.g_questionList.size();
            int percentage = (score * 100) / totalQuestions;
            
            Toast.makeText(this, 
                String.format("Quiz completed! Score: %d/%d (%d%%)", score, totalQuestions, percentage), 
                Toast.LENGTH_LONG).show();
            
            // TODO: Save score to database and navigate to results screen
            finish();
        }
    }

    private void showQuestionGrid() {
        // Show dialog with question grid for quick navigation
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Question Navigator");
        
        // Create the dialog first
        android.app.AlertDialog dialog = builder.create();
        
        // Create a simple grid view of question numbers
        android.widget.GridView gridView = new android.widget.GridView(this);
        gridView.setNumColumns(5);
        gridView.setAdapter(new android.widget.ArrayAdapter<String>(this, 
            android.R.layout.simple_list_item_1, 
            java.util.Arrays.asList(generateQuestionNumbers())) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView textView = new android.widget.TextView(QuestionsActivity.this);
                textView.setText(String.valueOf(position + 1));
                textView.setGravity(android.view.Gravity.CENTER);
                textView.setPadding(16, 16, 16, 16);
                textView.setBackgroundResource(R.drawable.round_cornor);
                textView.setTextColor(position == currentQuestionIndex ? 
                    getResources().getColor(R.color.white) : 
                    getResources().getColor(R.color.text_primary));
                textView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    position == currentQuestionIndex ? 
                    getResources().getColor(R.color.primary) : 
                    getResources().getColor(R.color.border_light)));
                
                textView.setOnClickListener(v -> {
                    currentQuestionIndex = position;
                    questionView.smoothScrollToPosition(position);
                    updateQuestionCounter();
                    dialog.dismiss();
                });
                
                return textView;
            }
        });
        
        builder.setView(gridView);
        builder.setNegativeButton("Close", (d, which) -> dialog.dismiss());
        
        dialog.show();
    }

    private String[] generateQuestionNumbers() {
        String[] numbers = new String[totalQuestions];
        for (int i = 0; i < totalQuestions; i++) {
            numbers[i] = String.valueOf(i + 1);
        }
        return numbers;
    }

    @Override
    public void onBackPressed() {
        // Show confirmation before exiting
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Exit Quiz")
                .setMessage("Are you sure you want to exit? Your progress will be lost.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Continue", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}