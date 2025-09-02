package com.example.quizpractice;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {

    private List<QuestionModel> questionsList;
    private List<Integer> selectedAnswers; // Store selected answers for each question
    private List<Boolean> bookmarkedQuestions; // Store bookmarked questions
    private List<Boolean> markedForReview; // Store questions marked for review
    private List<Boolean> visitedQuestions; // Store visited questions

    public QuestionAdapter(List<QuestionModel> questionsList) {
        this.questionsList = questionsList;
        this.selectedAnswers = new ArrayList<>();
        this.bookmarkedQuestions = new ArrayList<>();
        this.markedForReview = new ArrayList<>();
        this.visitedQuestions = new ArrayList<>();
        
        // Initialize arrays
        for (int i = 0; i < questionsList.size(); i++) {
            selectedAnswers.add(-1); // -1 means no answer selected
            bookmarkedQuestions.add(false);
            markedForReview.add(false);
            visitedQuestions.add(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return questionsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView ques;
        private Button optionA, optionB, optionC, optionD;
        private int currentPosition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ques = itemView.findViewById(R.id.Tv_question);
            optionA = itemView.findViewById(R.id.optionA);
            optionB = itemView.findViewById(R.id.optionB);
            optionC = itemView.findViewById(R.id.optionC);
            optionD = itemView.findViewById(R.id.optionD);
        }

        private void setData(final int pos) {
            currentPosition = pos;
            QuestionModel question = questionsList.get(pos);
            
            ques.setText(question.getQuestion());
            optionA.setText("A) " + question.getOptionA());
            optionB.setText("B) " + question.getOptionB());
            optionC.setText("C) " + question.getOptionC());
            optionD.setText("D) " + question.getOptionD());

            // Set click listeners for options
            optionA.setOnClickListener(v -> selectAnswer(pos, 0));
            optionB.setOnClickListener(v -> selectAnswer(pos, 1));
            optionC.setOnClickListener(v -> selectAnswer(pos, 2));
            optionD.setOnClickListener(v -> selectAnswer(pos, 3));

            // Update button states based on selection
            updateButtonStates(pos);
            
            // Log for debugging
            Log.d("QuestionAdapter", "Question " + (pos + 1) + " loaded: " + question.getQuestion());
            Log.d("QuestionAdapter", "Options: A=" + question.getOptionA() + ", B=" + question.getOptionB() + 
                ", C=" + question.getOptionC() + ", D=" + question.getOptionD());
        }

        private void selectAnswer(int questionPos, int answerIndex) {
            selectedAnswers.set(questionPos, answerIndex);
            updateButtonStates(questionPos);
        }

        private void updateButtonStates(int questionPos) {
            int selectedAnswer = selectedAnswers.get(questionPos);
            
            // Reset all buttons to default state
            resetButtonState(optionA);
            resetButtonState(optionB);
            resetButtonState(optionC);
            resetButtonState(optionD);

            // Highlight selected answer
            Button selectedButton = null;
            switch (selectedAnswer) {
                case 0:
                    selectedButton = optionA;
                    break;
                case 1:
                    selectedButton = optionB;
                    break;
                case 2:
                    selectedButton = optionC;
                    break;
                case 3:
                    selectedButton = optionD;
                    break;
            }

            if (selectedButton != null) {
                selectedButton.setSelected(true);
                selectedButton.setTextColor(Color.WHITE);
                // Add elevation for selected state
                selectedButton.setElevation(8f);
            }
        }

        private void resetButtonState(Button button) {
            button.setSelected(false);
            button.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
            button.setElevation(2f);
        }
    }

    // Public methods for external access
    public void setQuestionWidth(int width) {
        // This method can be used to set question width if needed
        Log.d("QuestionAdapter", "Question width set to: " + width);
    }
    
    public void clearSelection(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < selectedAnswers.size()) {
            selectedAnswers.set(questionIndex, -1);
            notifyItemChanged(questionIndex);
        }
    }

    public void markForReview(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < markedForReview.size()) {
            markedForReview.set(questionIndex, !markedForReview.get(questionIndex));
            notifyItemChanged(questionIndex);
        }
    }

    public void toggleBookmark(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < bookmarkedQuestions.size()) {
            bookmarkedQuestions.set(questionIndex, !bookmarkedQuestions.get(questionIndex));
            notifyItemChanged(questionIndex);
        }
    }

    // New methods for question navigation
    public void markQuestionAsVisited(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < visitedQuestions.size()) {
            visitedQuestions.set(questionIndex, true);
        }
    }

    public List<QuestionNavigationAdapter.QuestionState> getQuestionStates() {
        List<QuestionNavigationAdapter.QuestionState> states = new ArrayList<>();
        
        for (int i = 0; i < questionsList.size(); i++) {
            int state;
            if (markedForReview.get(i)) {
                state = QuestionNavigationAdapter.QuestionState.STATE_REVIEW;
            } else if (selectedAnswers.get(i) != -1) {
                state = QuestionNavigationAdapter.QuestionState.STATE_ANSWERED;
            } else if (visitedQuestions.get(i)) {
                state = QuestionNavigationAdapter.QuestionState.STATE_UNANSWERED;
            } else {
                state = QuestionNavigationAdapter.QuestionState.STATE_NOT_VISITED;
            }
            
            states.add(new QuestionNavigationAdapter.QuestionState(i, state));
        }
        
        return states;
    }

    public int getAnsweredCount() {
        int count = 0;
        for (Integer answer : selectedAnswers) {
            if (answer != -1) {
                count++;
            }
        }
        return count;
    }

    public int getUnansweredCount() {
        int count = 0;
        for (int i = 0; i < questionsList.size(); i++) {
            if (selectedAnswers.get(i) == -1 && visitedQuestions.get(i) && !markedForReview.get(i)) {
                count++;
            }
        }
        return count;
    }

    public int getNotVisitedCount() {
        int count = 0;
        for (Boolean visited : visitedQuestions) {
            if (!visited) {
                count++;
            }
        }
        return count;
    }

    public int getReviewCount() {
        int count = 0;
        for (Boolean review : markedForReview) {
            if (review) {
                count++;
            }
        }
        return count;
    }

    public int calculateScore() {
        int score = 0;
        Log.d("QuestionAdapter", "=== CALCULATING SCORE ===");
        Log.d("QuestionAdapter", "Total questions: " + questionsList.size());
        
        for (int i = 0; i < questionsList.size(); i++) {
            int selectedAnswer = selectedAnswers.get(i); // 0-3 from UI
            int correctAnswer = questionsList.get(i).getCorrectAnswer(); // 1-4 from database
            
            Log.d("QuestionAdapter", "Question " + (i + 1) + ":");
            Log.d("QuestionAdapter", "  - Selected: " + selectedAnswer + " (Option " + getOptionLetter(selectedAnswer) + ")");
            Log.d("QuestionAdapter", "  - Correct DB: " + correctAnswer + " (Option " + getOptionLetterFromDB(correctAnswer) + ")");
            Log.d("QuestionAdapter", "  - Question: " + questionsList.get(i).getQuestion());
            
            // Convert database index (1-4) to UI index (0-3) for comparison
            int correctAnswerUI = correctAnswer - 1;
            
            if (selectedAnswer == correctAnswerUI) {
                score++;
                Log.d("QuestionAdapter", "  - Match: YES (+1 point)");
            } else {
                Log.d("QuestionAdapter", "  - Match: NO (0 points)");
            }
        }
        
        Log.d("QuestionAdapter", "Final score: " + score + "/" + questionsList.size());
        Log.d("QuestionAdapter", "========================");
        return score;
    }
    
    private String getOptionLetter(int index) {
        switch (index) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return "NONE";
        }
    }
    
    private String getOptionLetterFromDB(int dbIndex) {
        switch (dbIndex) {
            case 1: return "A";
            case 2: return "B";
            case 3: return "C";
            case 4: return "D";
            default: return "INVALID";
        }
    }

    public List<Integer> getSelectedAnswers() {
        return new ArrayList<>(selectedAnswers);
    }

    public List<Boolean> getBookmarkedQuestions() {
        return new ArrayList<>(bookmarkedQuestions);
    }

    public List<Boolean> getMarkedForReview() {
        return new ArrayList<>(markedForReview);
    }

    public int getAnsweredQuestionsCount() {
        int count = 0;
        for (Integer answer : selectedAnswers) {
            if (answer != -1) {
                count++;
            }
        }
        return count;
    }

    // Get selected answer for a specific question
    public int getSelectedAnswer(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < selectedAnswers.size()) {
            return selectedAnswers.get(questionIndex);
        }
        return -1;
    }

    // Clear all selections (for re-attempt)
    public void clearAllSelections() {
        for (int i = 0; i < selectedAnswers.size(); i++) {
            selectedAnswers.set(i, -1);
        }
        notifyDataSetChanged();
    }
}
