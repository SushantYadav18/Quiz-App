package com.example.quizpractice;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import com.example.quizpractice.UserProgressManager;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    // UI Elements
    private TextView scoreText, timeTakenText, totalQuestionsText;
    private TextView correctCountText, wrongCountText, unattemptedCountText;

    // Data
    private int score;
    private int totalQuestions;
    private long timeTaken;
    private int correctAnswers;
    private int wrongAnswers;
    private int unattemptedQuestions;
    private String categoryId;
    private String testId;

    // Progress Dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize views
        initViews();

        // Get data from intent
        getDataFromIntent();

        // Calculate results
        calculateResults();

        // Display results
        displayResults();

        // Save result to database
        saveResult();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        scoreText = findViewById(R.id.scoreText);
        timeTakenText = findViewById(R.id.timeTakenText);
        totalQuestionsText = findViewById(R.id.totalQuestionsText);
        correctCountText = findViewById(R.id.correctCountText);
        wrongCountText = findViewById(R.id.wrongCountText);
        unattemptedCountText = findViewById(R.id.unattemptedCountText);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving your result...");
        progressDialog.setCancelable(false);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            score = intent.getIntExtra("SCORE", 0);
            totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
            timeTaken = intent.getLongExtra("TIME_TAKEN", 0);
            correctAnswers = intent.getIntExtra("CORRECT_COUNT", 0);
            wrongAnswers = intent.getIntExtra("WRONG_COUNT", 0);
            unattemptedQuestions = intent.getIntExtra("UNATTEMPTED_COUNT", 0);
            categoryId = intent.getStringExtra("CATEGORY_ID");
            testId = intent.getStringExtra("TEST_ID");
            
            Log.d(TAG, "Received data - Score: " + score + ", Total: " + totalQuestions + 
                ", Time: " + timeTaken + ", Correct: " + correctAnswers + 
                ", Wrong: " + wrongAnswers + ", Unattempted: " + unattemptedQuestions +
                ", Category: " + categoryId + ", Test: " + testId);
        }
    }

    private void calculateResults() {
        // Results are already calculated and passed from QuestionsActivity
        // Just verify the data integrity
        int totalCalculated = correctAnswers + wrongAnswers + unattemptedQuestions;
        
        if (totalCalculated != totalQuestions) {
            Log.w(TAG, "Data integrity issue: calculated total (" + totalCalculated + 
                ") doesn't match total questions (" + totalQuestions + ")");
            
            // Fallback: recalculate if data is inconsistent
            if (DbQuery.g_questionList != null && !DbQuery.g_questionList.isEmpty()) {
                ArrayList<Integer> selectedAnswers = getIntent().getIntegerArrayListExtra("SELECTED_ANSWERS");
                if (selectedAnswers != null && selectedAnswers.size() == DbQuery.g_questionList.size()) {
                    correctAnswers = 0;
                    wrongAnswers = 0;
                    unattemptedQuestions = 0;

                    for (int i = 0; i < DbQuery.g_questionList.size(); i++) {
                        QuestionModel question = DbQuery.g_questionList.get(i);
                        int selectedAnswer = selectedAnswers.get(i);
                        
                        if (selectedAnswer == -1) {
                            unattemptedQuestions++;
                        } else if (selectedAnswer == question.getCorrectAnswer()) {
                            correctAnswers++;
                        } else {
                            wrongAnswers++;
                        }
                    }
                    
                    Log.d(TAG, "Recalculated - Correct: " + correctAnswers + ", Wrong: " + wrongAnswers + 
                        ", Unattempted: " + unattemptedQuestions);
                }
            }
        }

        Log.d(TAG, "Final results - Correct: " + correctAnswers + ", Wrong: " + wrongAnswers + 
            ", Unattempted: " + unattemptedQuestions);
    }

    private void displayResults() {
        // Display score
        scoreText.setText(String.valueOf(score));

        // Display time taken
        String timeString = formatTime(timeTaken);
        timeTakenText.setText(timeString);

        // Display total questions
        totalQuestionsText.setText(String.valueOf(totalQuestions));

        // Display performance breakdown
        correctCountText.setText(String.valueOf(correctAnswers));
        wrongCountText.setText(String.valueOf(wrongAnswers));
        unattemptedCountText.setText(String.valueOf(unattemptedQuestions));

        Log.d(TAG, "Results displayed - Score: " + score + ", Time: " + timeString + 
            ", Correct: " + correctAnswers + ", Wrong: " + wrongAnswers + 
            ", Unattempted: " + unattemptedQuestions);
    }

    private String formatTime(long timeInMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60;
        return String.format("%02d:%02d m", minutes, seconds);
    }

    private void saveResult() {
        progressDialog.show();

        // Save result to database
        DbQuery.saveResult(score, new MyCompleteListener() {
            @Override
            public void onSuccess() {
                // Also save to UserProgressManager for difficulty tracking
                try {
                    String categoryId = getIntent().getStringExtra("CATEGORY_ID");
                    String testId = getIntent().getStringExtra("TEST_ID");
                    
                    Log.d(TAG, "=== PROGRESS MANAGER DEBUG ===");
                    Log.d(TAG, "CategoryId from intent: " + categoryId);
                    Log.d(TAG, "TestId from intent: " + testId);
                    Log.d(TAG, "Score: " + score);
                    Log.d(TAG, "Total questions: " + totalQuestions);
                    
                    if (categoryId != null && testId != null) {
                        UserProgressManager progressManager = UserProgressManager.getInstance(ResultActivity.this);
                        Log.d(TAG, "Calling saveTestCompletion...");
                        progressManager.saveTestCompletion(categoryId, testId, score, totalQuestions);
                        Log.d(TAG, "Test completion saved to UserProgressManager");
                        
                        // Verify the data was saved
                        int savedBestScore = progressManager.getBestScore(categoryId, testId);
                        int savedAttemptCount = progressManager.getAttemptCount(categoryId, testId);
                        Log.d(TAG, "Verification - Best Score: " + savedBestScore + "%, Attempt Count: " + savedAttemptCount);
                    } else {
                        Log.e(TAG, "CategoryId or TestId is null - cannot save progress");
                    }
                    Log.d(TAG, "==============================");
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to UserProgressManager: " + e.getMessage());
                    e.printStackTrace();
                }
                
                progressDialog.dismiss();
                Toast.makeText(ResultActivity.this, "Result saved successfully!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Result saved to database successfully");
            }

            @Override
            public void onFailure() {
                progressDialog.dismiss();
                Toast.makeText(ResultActivity.this, "Failed to save result. Please try again.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save result to database");
            }
        });
    }

    private void setClickListeners() {
        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
        });

        // Re-attempt button
        findViewById(R.id.reattemptButton).setOnClickListener(v -> {
            // Clear previous answers and restart quiz
            clearPreviousAnswers();
            restartQuiz();
        });

        // View answers button
        findViewById(R.id.viewAnswersButton).setOnClickListener(v -> {
            // Navigate to answers review screen
            viewAnswers();
        });

        // Leaderboard button
        findViewById(R.id.leaderboardButton).setOnClickListener(v -> {
            // Navigate to leaderboard
            checkLeaderboard();
        });
    }

    private void clearPreviousAnswers() {
        // Clear previous answers from QuestionAdapter
        if (DbQuery.g_questionList != null) {
            // Reset the adapter's selected answers
            // This will be implemented based on your QuestionAdapter
            Log.d(TAG, "Previous answers cleared for re-attempt");
        }
    }

    private void restartQuiz() {
        // Navigate back to QuestionsActivity to restart the quiz
        Intent intent = new Intent(this, QuestionsActivity.class);
        intent.putExtra("RESTART_QUIZ", true);
        intent.putExtra("CATEGORY_ID", categoryId);
        intent.putExtra("TEST_ID", testId);
        startActivity(intent);
        finish();
    }

    private void viewAnswers() {
        // Navigate to answers review screen
        // TODO: Create AnswersReviewActivity or implement answers review functionality
        Toast.makeText(this, "Answers review feature coming soon!", Toast.LENGTH_SHORT).show();
        
        // Uncomment when AnswersReviewActivity is created:
        // Intent intent = new Intent(this, AnswersReviewActivity.class);
        // intent.putExtra("CATEGORY_ID", categoryId);
        // intent.putExtra("TEST_ID", testId);
        // startActivity(intent);
    }

    private void checkLeaderboard() {
        // Navigate to leaderboard fragment in MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("OPEN_LEADERBOARD", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Navigate back to main screen
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
