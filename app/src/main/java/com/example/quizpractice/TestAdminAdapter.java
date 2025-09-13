package com.example.quizpractice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TestAdminAdapter extends RecyclerView.Adapter<TestAdminAdapter.TestViewHolder> {
    
    private List<TestAdminModel> testList;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private OnManageQuestionsClickListener onManageQuestionsClickListener;
    
    public interface OnEditClickListener {
        void onEditClick(TestAdminModel test);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(TestAdminModel test);
    }
    
    public interface OnManageQuestionsClickListener {
        void onManageQuestionsClick(TestAdminModel test);
    }
    
    public TestAdminAdapter(List<TestAdminModel> testList) {
        this.testList = testList;
    }
    
    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }
    
    public void setOnManageQuestionsClickListener(OnManageQuestionsClickListener listener) {
        this.onManageQuestionsClickListener = listener;
    }
    
    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
        return new TestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        TestAdminModel test = testList.get(position);
        holder.testNameText.setText(test.getName());
        holder.testTimeText.setText("Time: " + test.getTime() + " minutes");
        holder.questionCountText.setText("Questions: " + test.getQuestionCount());
        
        holder.editTestBtn.setOnClickListener(v -> {
            if (onEditClickListener != null) {
                onEditClickListener.onEditClick(test);
            }
        });
        
        holder.deleteTestBtn.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(test);
            }
        });
        
        holder.manageQuestionsBtn.setOnClickListener(v -> {
            if (onManageQuestionsClickListener != null) {
                onManageQuestionsClickListener.onManageQuestionsClick(test);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return testList.size();
    }
    
    static class TestViewHolder extends RecyclerView.ViewHolder {
        TextView testNameText;
        TextView testTimeText;
        TextView questionCountText;
        ImageButton editTestBtn;
        ImageButton deleteTestBtn;
        ImageButton manageQuestionsBtn;
        
        public TestViewHolder(@NonNull View itemView) {
            super(itemView);
            testNameText = itemView.findViewById(R.id.testNameText);
            testTimeText = itemView.findViewById(R.id.testTimeText);
            questionCountText = itemView.findViewById(R.id.questionCountText);
            editTestBtn = itemView.findViewById(R.id.editTestBtn);
            deleteTestBtn = itemView.findViewById(R.id.deleteTestBtn);
            manageQuestionsBtn = itemView.findViewById(R.id.manageQuestionsBtn);
        }
    }
}