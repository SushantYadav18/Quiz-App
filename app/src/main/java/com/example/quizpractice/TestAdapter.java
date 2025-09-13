package com.example.quizpractice;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizpractice.UserProgressManager;

import java.util.List;
import java.util.Map;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder> {
    private static final String TAG = "TestAdapter";
    private List<TestModel> testList;
    private Context context;

    public TestAdapter(List<TestModel> testList, Context context) {
        this.testList = testList;
        this.context = context;
        Log.d(TAG, "Adapter created with " + (testList != null ? testList.size() : 0) + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_item_layout, parent, false);
        Log.d(TAG, "Creating new ViewHolder");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (testList != null && position < testList.size()) {
                TestModel model = testList.get(position);
                Log.d(TAG, "Binding item at position " + position + ": " + model.getId());
                
                // Get progress data from UserProgressManager
                UserProgressManager progressManager = UserProgressManager.getInstance(context);
                String categoryId = DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getDocID();
                Map<String, Object> progressData = progressManager.getTestProgressData(categoryId, model.getId());
                
                int bestScore = (Integer) progressData.get("best_score");
                int attemptCount = (Integer) progressData.get("attempt_count");
                boolean isCompleted = (Boolean) progressData.get("is_completed");
                
                Log.d(TAG, "Progress data for Test " + model.getId() + ": Best Score = " + bestScore + "%, Attempts = " + attemptCount);
                
                // Set test number
                holder.testNo.setText("Test " + model.getId());
                
                // Set difficulty level
                holder.difficultyText.setText(model.getDifficultyText());
                holder.difficultyText.setTextColor(model.getDifficultyColor());
                
                // Set best score with proper formatting
                holder.topScore.setText(bestScore + "%");
                
                // Set progress bar based on best score
                holder.progressBar.setMax(100);
                holder.progressBar.setProgress(bestScore);

                // Color progress bar based on completion
                if (bestScore >= 70) {
                    holder.progressBar.getProgressDrawable().setColorFilter(
                        context.getResources().getColor(R.color.success), PorterDuff.Mode.SRC_IN);
                } else if (bestScore >= 50) {
                    holder.progressBar.getProgressDrawable().setColorFilter(
                        context.getResources().getColor(R.color.accent_yellow), PorterDuff.Mode.SRC_IN);
                } else {
                    holder.progressBar.getProgressDrawable().setColorFilter(
                        context.getResources().getColor(R.color.error), PorterDuff.Mode.SRC_IN);
                }

                // Set time
                holder.timeText.setText(model.getTime() + " min");

                // Handle locked/unlocked state
                if (model.isUnlocked()) {
                    // Test is unlocked - show normal state
                    holder.itemView.setEnabled(true);
                    holder.itemView.setAlpha(1.0f);
                    holder.lockIcon.setVisibility(View.GONE);
                    holder.unlockText.setVisibility(View.GONE);
                    
                    // Show normal colors
                    holder.cardView.setCardBackgroundColor(Color.WHITE);
                    holder.testNo.setTextColor(Color.BLACK);
                    holder.topScore.setTextColor(Color.BLACK);
                    holder.timeText.setTextColor(Color.BLACK);
                    
                    // Add click animation and listener
                    holder.itemView.setOnClickListener(v -> {
                        v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                                // Navigate to Start Test page, then Questions
                                DbQuery.g_selected_test_index = position;
                                Intent intent = new Intent(context, test_startActivity.class);
                                context.startActivity(intent);
                            })
                            .start();
                    });
                } else {
                    // Test is locked - show locked state
                    holder.itemView.setEnabled(false);
                    holder.itemView.setAlpha(0.6f);
                    holder.lockIcon.setVisibility(View.VISIBLE);
                    holder.unlockText.setVisibility(View.VISIBLE);
                    
                    // Show locked colors
                    holder.cardView.setCardBackgroundColor(Color.LTGRAY);
                    holder.testNo.setTextColor(Color.GRAY);
                    holder.topScore.setTextColor(Color.GRAY);
                    holder.timeText.setTextColor(Color.GRAY);
                    
                    // Set unlock requirement text
                    String unlockText = "";
                    String testCategoryId = DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getDocID();
                    
                    if (model.getDifficulty().equals("MEDIUM")) {
                        // Get overall easy tests completion
                        int easyTestsCount = 0;
                        int easyTestsCompleted = 0;
                        int totalEasyScore = 0;
                        
                        for (TestModel test : testList) {
                            if (test.getDifficulty().equals("EASY")) {
                                easyTestsCount++;
                                int easyScore = progressManager.getBestScore(testCategoryId, test.getId());
                                totalEasyScore += easyScore;
                                
                                if (easyScore >= 70) {
                                    easyTestsCompleted++;
                                }
                            }
                        }
                        
                        int overallEasyCompletion = (easyTestsCount > 0) ? (totalEasyScore / easyTestsCount) : 0;
                        
                        unlockText = String.format("Complete ALL easy tests with at least 70%% score and achieve 70%% overall average to unlock\nCurrent progress: %d/%d tests completed, %d%% overall", 
                            easyTestsCompleted, easyTestsCount, overallEasyCompletion);
                    } else if (model.getDifficulty().equals("HARD")) {
                        // Get overall medium tests completion
                        int mediumTestsCount = 0;
                        int mediumTestsCompleted = 0;
                        int totalMediumScore = 0;
                        
                        for (TestModel test : testList) {
                            if (test.getDifficulty().equals("MEDIUM")) {
                                mediumTestsCount++;
                                int mediumScore = progressManager.getBestScore(testCategoryId, test.getId());
                                totalMediumScore += mediumScore;
                                
                                if (mediumScore >= 70) {
                                    mediumTestsCompleted++;
                                }
                            }
                        }
                        
                        int overallMediumCompletion = (mediumTestsCount > 0) ? (totalMediumScore / mediumTestsCount) : 0;
                        
                        unlockText = String.format("Complete ALL medium tests with at least 70%% score and achieve 70%% overall average to unlock\nCurrent progress: %d/%d tests completed, %d%% overall", 
                            mediumTestsCompleted, mediumTestsCount, overallMediumCompletion);
                    } else {
                        unlockText = "Complete previous tests to unlock";
                    }
                    holder.unlockText.setText(unlockText);
                    
                    // Remove click listener for locked tests
                    holder.itemView.setOnClickListener(null);
                }

                // Make all views visible
                holder.testNo.setVisibility(View.VISIBLE);
                holder.topScore.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.timeText.setVisibility(View.VISIBLE);
                holder.difficultyText.setVisibility(View.VISIBLE);
                holder.itemView.setVisibility(View.VISIBLE);

            } else {
                Log.e(TAG, "Invalid position or null list: position=" + position + ", list size=" + (testList != null ? testList.size() : 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        int count = testList != null ? testList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView testNo;
        TextView topScore;
        TextView timeText;
        TextView difficultyText;
        TextView unlockText;
        ProgressBar progressBar;
        ImageView lockIcon;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            testNo = itemView.findViewById(R.id.testNum);
            topScore = itemView.findViewById(R.id.Scoretext);
            timeText = itemView.findViewById(R.id.timeText);
            progressBar = itemView.findViewById(R.id.testprogressBar);
            difficultyText = itemView.findViewById(R.id.difficultyText);
            unlockText = itemView.findViewById(R.id.unlockText);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            cardView = itemView.findViewById(R.id.testCard);

            if (testNo == null) Log.e("ViewHolder", "testNo TextView not found");
            if (topScore == null) Log.e("ViewHolder", "topScore TextView not found");
            if (timeText == null) Log.e("ViewHolder", "timeText TextView not found");
            if (progressBar == null) Log.e("ViewHolder", "progressBar not found");
            if (difficultyText == null) Log.e("ViewHolder", "difficultyText TextView not found");
            if (unlockText == null) Log.e("ViewHolder", "unlockText TextView not found");
            if (lockIcon == null) Log.e("ViewHolder", "lockIcon ImageView not found");
            if (cardView == null) Log.e("ViewHolder", "testCard CardView not found");
        }

        public void setData(int pos, int progress) {
            testNo.setText("Test No" + String.valueOf(pos + 1));
            topScore.setText(String.valueOf(progress) + "%");

            progressBar.setProgress(progress);

            itemView.setOnClickListener((view) -> {
                DbQuery.g_selected_test_index =pos;
                        Intent intent = new Intent(itemView.getContext(), test_startActivity.class);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
