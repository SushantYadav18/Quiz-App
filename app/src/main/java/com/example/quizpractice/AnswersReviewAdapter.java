package com.example.quizpractice;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AnswersReviewAdapter extends RecyclerView.Adapter<AnswersReviewAdapter.ViewHolder> {

    private static final String TAG = "AnswersReviewAdapter";
    private List<QuestionModel> questionsList;
    private ArrayList<Integer> selectedAnswers;

    public AnswersReviewAdapter(List<QuestionModel> questionsList, ArrayList<Integer> selectedAnswers) {
        this.questionsList = questionsList;
        this.selectedAnswers = selectedAnswers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.answer_review_item, parent, false);
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

        private TextView questionNumber, questionText;
        private RadioGroup optionsGroup;
        private RadioButton optionA, optionB, optionC, optionD;
        private TextView yourAnswer, correctAnswer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            questionNumber = itemView.findViewById(R.id.question_number);
            questionText = itemView.findViewById(R.id.question_text);
            optionsGroup = itemView.findViewById(R.id.options_group);
            optionA = itemView.findViewById(R.id.option_a);
            optionB = itemView.findViewById(R.id.option_b);
            optionC = itemView.findViewById(R.id.option_c);
            optionD = itemView.findViewById(R.id.option_d);
            yourAnswer = itemView.findViewById(R.id.your_answer);
            correctAnswer = itemView.findViewById(R.id.correct_answer);
        }

        private void setData(int position) {
            // Get question and user's answer
            QuestionModel question = questionsList.get(position);
            int originalIndex = findOriginalQuestionIndex(question);
            int selectedAnswer = -1;
            
            if (originalIndex != -1 && originalIndex < selectedAnswers.size()) {
                selectedAnswer = selectedAnswers.get(originalIndex);
            }
            
            // Set question number and text
            questionNumber.setText("Question " + (position + 1));
            questionText.setText(question.getQuestion());
            
            // Set options
            optionA.setText("A) " + question.getOptionA());
            optionB.setText("B) " + question.getOptionB());
            optionC.setText("C) " + question.getOptionC());
            optionD.setText("D) " + question.getOptionD());
            
            // Get correct answer (1-4 from database, convert to 0-3 for UI)
            int correctAnswerIndex = question.getCorrectAnswer() - 1;
            
            // Highlight correct answer
            highlightCorrectAnswer(correctAnswerIndex);
            
            // Show user's answer
            if (selectedAnswer == -1) {
                yourAnswer.setText("Not attempted");
                yourAnswer.setTextColor(Color.GRAY);
            } else {
                String userAnswerText = getOptionLetter(selectedAnswer) + ") " + getOptionText(question, selectedAnswer);
                yourAnswer.setText(userAnswerText);
                
                // Set color based on correctness
                if (selectedAnswer == correctAnswerIndex) {
                    yourAnswer.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    yourAnswer.setTextColor(Color.parseColor("#F44336")); // Red
                }
            }
            
            // Show correct answer
            String correctAnswerText = getOptionLetter(correctAnswerIndex) + ") " + 
                    getOptionText(question, correctAnswerIndex);
            correctAnswer.setText(correctAnswerText);
        }
        
        private int findOriginalQuestionIndex(QuestionModel question) {
            // Find the original index of this question in DbQuery.g_questionList
            if (DbQuery.g_questionList != null) {
                for (int i = 0; i < DbQuery.g_questionList.size(); i++) {
                    QuestionModel originalQuestion = DbQuery.g_questionList.get(i);
                    if (question.getQuestion().equals(originalQuestion.getQuestion())) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private void highlightCorrectAnswer(int correctAnswerIndex) {
            // Reset all options
            optionA.setChecked(false);
            optionB.setChecked(false);
            optionC.setChecked(false);
            optionD.setChecked(false);
            
            optionA.setTextColor(Color.BLACK);
            optionB.setTextColor(Color.BLACK);
            optionC.setTextColor(Color.BLACK);
            optionD.setTextColor(Color.BLACK);
            
            // Highlight correct answer
            RadioButton correctOption = null;
            switch (correctAnswerIndex) {
                case 0:
                    correctOption = optionA;
                    break;
                case 1:
                    correctOption = optionB;
                    break;
                case 2:
                    correctOption = optionC;
                    break;
                case 3:
                    correctOption = optionD;
                    break;
            }
            
            if (correctOption != null) {
                correctOption.setChecked(true);
                correctOption.setTextColor(Color.parseColor("#4CAF50")); // Green
            }
        }

        private String getOptionLetter(int index) {
            switch (index) {
                case 0: return "A";
                case 1: return "B";
                case 2: return "C";
                case 3: return "D";
                default: return "?";
            }
        }

        private String getOptionText(QuestionModel question, int optionIndex) {
            switch (optionIndex) {
                case 0: return question.getOptionA();
                case 1: return question.getOptionB();
                case 2: return question.getOptionC();
                case 3: return question.getOptionD();
                default: return "Unknown";
            }
        }
    }
}