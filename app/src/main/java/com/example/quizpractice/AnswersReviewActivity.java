package com.example.quizpractice;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnswersReviewActivity extends AppCompatActivity {

    private static final String TAG = "AnswersReviewActivity";

    // UI Elements
    private RecyclerView answersRecyclerView;
    private Button prevButton, nextButton;
    private TextView questionCounter;
    private Toolbar toolbar;

    // Data
    private List<QuestionModel> questionsList;
    private ArrayList<Integer> selectedAnswers;
    private int currentPosition = 0;
    private int totalQuestions = 0;
    private AnswersReviewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answers_review);

        // Initialize views
        initViews();

        // Get data from intent
        getDataFromIntent();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup navigation
        setupNavigation();
    }

    private void initViews() {
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize other views
        answersRecyclerView = findViewById(R.id.answers_recycler_view);
        prevButton = findViewById(R.id.prev_button);
        nextButton = findViewById(R.id.next_button);
        questionCounter = findViewById(R.id.question_counter);
    }

    private void getDataFromIntent() {
        // Get selected answers from intent
        selectedAnswers = getIntent().getIntegerArrayListExtra("SELECTED_ANSWERS");
        
        // Get questions list from DbQuery
        if (DbQuery.g_questionList != null && !DbQuery.g_questionList.isEmpty()) {
            // Create a copy of the questions list to avoid modifying the original
            questionsList = new ArrayList<>(DbQuery.g_questionList);
            totalQuestions = questionsList.size();
            
            // Shuffle the questions list
            Collections.shuffle(questionsList);
            
            Log.d(TAG, "Questions loaded and shuffled: " + totalQuestions + " questions");
        } else {
            Log.e(TAG, "No questions found in DbQuery");
            Toast.makeText(this, "Error: No questions found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        // Create adapter
        adapter = new AnswersReviewAdapter(questionsList, selectedAnswers);
        
        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        answersRecyclerView.setLayoutManager(layoutManager);
        
        // Set adapter
        answersRecyclerView.setAdapter(adapter);
        
        // Add snap helper for paging effect
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(answersRecyclerView);
        
        // Add scroll listener to update counter
        answersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        currentPosition = position;
                        updateQuestionCounter();
                    }
                }
            }
        });
        
        // Initial counter update
        updateQuestionCounter();
    }

    private void setupNavigation() {
        // Previous button
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPosition > 0) {
                    currentPosition--;
                    answersRecyclerView.smoothScrollToPosition(currentPosition);
                    updateQuestionCounter();
                }
            }
        });
        
        // Next button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPosition < totalQuestions - 1) {
                    currentPosition++;
                    answersRecyclerView.smoothScrollToPosition(currentPosition);
                    updateQuestionCounter();
                }
            }
        });
    }

    private void updateQuestionCounter() {
        questionCounter.setText((currentPosition + 1) + "/" + totalQuestions);
        
        // Update button states
        prevButton.setEnabled(currentPosition > 0);
        nextButton.setEnabled(currentPosition < totalQuestions - 1);
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