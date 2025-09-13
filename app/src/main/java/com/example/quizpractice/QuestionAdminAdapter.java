package com.example.quizpractice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuestionAdminAdapter extends RecyclerView.Adapter<QuestionAdminAdapter.QuestionViewHolder> {
    
    private List<QuestionAdminModel> questionList;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    
    public interface OnEditClickListener {
        void onEditClick(QuestionAdminModel question);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(QuestionAdminModel question);
    }
    
    public QuestionAdminAdapter(List<QuestionAdminModel> questionList) {
        this.questionList = questionList;
    }
    
    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }
    
    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        QuestionAdminModel question = questionList.get(position);
        holder.questionNumberText.setText("Q" + (position + 1) + ".");
        holder.questionText.setText(question.getQuestion());
        holder.optionAText.setText("A) " + question.getOptionA());
        holder.optionBText.setText("B) " + question.getOptionB());
        holder.optionCText.setText("C) " + question.getOptionC());
        holder.optionDText.setText("D) " + question.getOptionD());
        holder.correctAnswerText.setText("Correct Answer: " + question.getCorrectAnswer());
        
        holder.editQuestionBtn.setOnClickListener(v -> {
            if (onEditClickListener != null) {
                onEditClickListener.onEditClick(question);
            }
        });
        
        holder.deleteQuestionBtn.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(question);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return questionList.size();
    }
    
    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView questionNumberText;
        TextView questionText;
        TextView optionAText;
        TextView optionBText;
        TextView optionCText;
        TextView optionDText;
        TextView correctAnswerText;
        ImageButton editQuestionBtn;
        ImageButton deleteQuestionBtn;
        
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            questionNumberText = itemView.findViewById(R.id.questionNumberText);
            questionText = itemView.findViewById(R.id.questionText);
            optionAText = itemView.findViewById(R.id.optionAText);
            optionBText = itemView.findViewById(R.id.optionBText);
            optionCText = itemView.findViewById(R.id.optionCText);
            optionDText = itemView.findViewById(R.id.optionDText);
            correctAnswerText = itemView.findViewById(R.id.correctAnswerText);
            editQuestionBtn = itemView.findViewById(R.id.editQuestionBtn);
            deleteQuestionBtn = itemView.findViewById(R.id.deleteQuestionBtn);
        }
    }
}