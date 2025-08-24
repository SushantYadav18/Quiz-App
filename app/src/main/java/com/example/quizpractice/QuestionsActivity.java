package com.example.quizpractice;

import static com.example.quizpractice.DbQuery.g_selected_test_index;
import static com.example.quizpractice.DbQuery.g_testList;

import android.os.Bundle;
import android.os.CountDownTimer;
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

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.List;

// Custom ItemDecoration to ensure proper question sizing
class QuestionItemDecoration extends RecyclerView.ItemDecoration {
    private final int screenWidth;

    public QuestionItemDecoration(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    @Override
    public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // Ensure each question takes full screen width minus margins
        int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.left = 0;
        } else {
            outRect.left = 0;
        }
        outRect.right = 0;
    }
}

public class QuestionsActivity extends AppCompatActivity {

    private RecyclerView questionView;
    private TextView tvQuesID, timerTV, catNameTV;
    private Button submitB, markB, clearSelB;
    private ImageButton prevQuesB, nextQuesB;
    private ImageView quesListB, bookmarkB;

    private QuestionAdapter questionAdapter;
    private int currentQuestionIndex = 0;
    private int totalQuestions = 0;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_questions);

        init();
        setupQuestionData();
        setupClickListeners();
        startTimer();

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
        if (g_testList != null && !g_testList.isEmpty() &&
            DbQuery.g_selected_test_index < g_testList.size()) {
            int timeInMinutes = g_testList.get(DbQuery.g_selected_test_index).getTime();
            timerTV.setText(String.format("%02d:00 min", timeInMinutes));
            Log.d("QuestionsActivity", "Test time: " + timeInMinutes + " minutes");
        }

        // Setup RecyclerView with proper sizing
        questionAdapter = new QuestionAdapter(DbQuery.g_questionList);
        questionView.setAdapter(questionAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        questionView.setLayoutManager(layoutManager);
        
        // Set item width to match screen width for better display
        questionView.addItemDecoration(new QuestionItemDecoration(getResources().getDisplayMetrics().widthPixels));
        
        // Ensure questions take proper width
        setQuestionWidth();

        // Update question counter
        updateQuestionCounter();

        // Scroll to first question
        questionView.scrollToPosition(0);
        
        // Mark first question as visited
        if (questionAdapter != null) {
            questionAdapter.markQuestionAsVisited(0);
        }

        Log.d("QuestionsActivity", "Questions setup completed successfully");
    }

    private void setQuestionWidth() {
        // Get screen width and set question item width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int questionWidth = screenWidth - 32; // Account for margins
        
        Log.d("QuestionsActivity", "Screen width: " + screenWidth + ", Question width: " + questionWidth);
        
        // Set the question width in the adapter if needed
        if (questionAdapter != null) {
            questionAdapter.setQuestionWidth(questionWidth);
        }
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
                    // Add sample questions for testing
                    addSampleQuestions();
                    setupQuestionData();
                }
            }

            @Override
            public void onFailure() {
                loadingDialog.dismiss();
                Log.e("QuestionsActivity", "Failed to load questions from database");
                // Add sample questions for testing
                addSampleQuestions();
                setupQuestionData();
                
                Toast.makeText(QuestionsActivity.this,
                    "Added sample questions for testing",
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addSampleQuestions() {
        // Create sample questions for testing the layout
        if (DbQuery.g_questionList == null) {
            DbQuery.g_questionList = new ArrayList<>();
        }
        
        DbQuery.g_questionList.clear();
        
        // Add sample questions
        DbQuery.g_questionList.add(new QuestionModel(
            "What is the capital of France?",
            "Paris", "London", "Berlin", "Madrid", 0
        ));
        
        DbQuery.g_questionList.add(new QuestionModel(
            "Which planet is known as the Red Planet?",
            "Earth", "Mars", "Jupiter", "Venus", 1
        ));
        
        DbQuery.g_questionList.add(new QuestionModel(
            "What is the largest ocean on Earth?",
            "Atlantic", "Indian", "Arctic", "Pacific", 3
        ));
        
        DbQuery.g_questionList.add(new QuestionModel(
            "Who wrote 'Romeo and Juliet'?",
            "Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain", 1
        ));
        
        DbQuery.g_questionList.add(new QuestionModel(
            "What is the chemical symbol for gold?",
            "Ag", "Au", "Fe", "Cu", 1
        ));
        
        Log.d("QuestionsActivity", "Added " + DbQuery.g_questionList.size() + " sample questions");
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
                    // Mark question as visited
                    if (questionAdapter != null) {
                        questionAdapter.markQuestionAsVisited(currentQuestionIndex);
                    }
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
                    // Mark question as visited
                    if (questionAdapter != null) {
                        questionAdapter.markQuestionAsVisited(currentQuestionIndex);
                    }
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
                            // Mark question as visited
                            if (questionAdapter != null) {
                                questionAdapter.markQuestionAsVisited(currentQuestionIndex);
                            }
                        }
                    }
                }
            }
        });
    }

    private void startTimer() {
        // Check if test data is available
        if (g_testList == null || g_testList.isEmpty() || 
            DbQuery.g_selected_test_index < 0 || 
            DbQuery.g_selected_test_index >= g_testList.size()) {
            Log.e("QuestionsActivity", "Invalid test data for timer");
            timerTV.setText("00:00 min");
            return;
        }

        // Get time in minutes and convert to milliseconds
        int timeInMinutes = g_testList.get(DbQuery.g_selected_test_index).getTime();
        long totalTime = timeInMinutes * 60 * 1000; // Convert minutes to milliseconds

        Log.d("QuestionsActivity", "Starting timer for " + timeInMinutes + " minutes");

        timer = new CountDownTimer(totalTime + 1000, 1000) {
            @Override
            public void onTick(long remainingTime) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60;
                String time = String.format("%02d:%02d min", minutes, seconds);
                timerTV.setText(time);
            }

            @Override
            public void onFinish() {
                timerTV.setText("00:00 min");
                Toast.makeText(QuestionsActivity.this, "Time's up! Submitting quiz...", Toast.LENGTH_LONG).show();
                submitQuiz();
            }
        };
        timer.start();
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
        // Cancel timer first
        cancelTimer();
        
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
        // Show the new question navigation dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.question_navigation_dialog, null);
        builder.setView(dialogView);

        // Create the dialog
        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        // Initialize views
        RecyclerView questionGrid = dialogView.findViewById(R.id.questionGrid);
        TextView answeredCount = dialogView.findViewById(R.id.answeredCount);
        TextView unansweredCount = dialogView.findViewById(R.id.unansweredCount);
        TextView notVisitedCount = dialogView.findViewById(R.id.notVisitedCount);
        TextView reviewCount = dialogView.findViewById(R.id.reviewCount);
        View closeButton = dialogView.findViewById(R.id.closeButton);

        // Set up question grid
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5); // 5 questions per row
        questionGrid.setLayoutManager(gridLayoutManager);

        // Get question states from adapter
        List<QuestionNavigationAdapter.QuestionState> questionStates = questionAdapter.getQuestionStates();
        QuestionNavigationAdapter navigationAdapter = new QuestionNavigationAdapter(this, questionStates, 
            new QuestionNavigationAdapter.OnQuestionClickListener() {
                @Override
                public void onQuestionClick(int position) {
                    // Navigate to the selected question
                    currentQuestionIndex = position;
                    questionView.smoothScrollToPosition(position);
                    updateQuestionCounter();
                    dialog.dismiss();
                }
            });
        
        questionGrid.setAdapter(navigationAdapter);

        // Update statistics
        answeredCount.setText(String.valueOf(questionAdapter.getAnsweredCount()));
        unansweredCount.setText(String.valueOf(questionAdapter.getUnansweredCount()));
        notVisitedCount.setText(String.valueOf(questionAdapter.getNotVisitedCount()));
        reviewCount.setText(String.valueOf(questionAdapter.getReviewCount()));

        // Set close button click listener
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
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
                    cancelTimer();
                    super.onBackPressed();
                })
                .setNegativeButton("Continue", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}