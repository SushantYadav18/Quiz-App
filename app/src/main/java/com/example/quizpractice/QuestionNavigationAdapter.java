package com.example.quizpractice;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuestionNavigationAdapter extends RecyclerView.Adapter<QuestionNavigationAdapter.ViewHolder> {

    private List<QuestionState> questionStates;
    private Context context;
    private OnQuestionClickListener listener;

    public interface OnQuestionClickListener {
        void onQuestionClick(int position);
    }

    public static class QuestionState {
        public static final int STATE_NOT_VISITED = 0;
        public static final int STATE_ANSWERED = 1;
        public static final int STATE_UNANSWERED = 2;
        public static final int STATE_REVIEW = 3;

        private int position;
        private int state;

        public QuestionState(int position, int state) {
            this.position = position;
            this.state = state;
        }

        public int getPosition() { return position; }
        public int getState() { return state; }
    }

    public QuestionNavigationAdapter(Context context, List<QuestionState> questionStates, OnQuestionClickListener listener) {
        this.context = context;
        this.questionStates = questionStates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.question_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionState questionState = questionStates.get(position);
        holder.bind(questionState);
    }

    @Override
    public int getItemCount() {
        return questionStates.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView questionNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionNumber = itemView.findViewById(R.id.questionNumber);
        }

        public void bind(QuestionState questionState) {
            questionNumber.setText(String.valueOf(questionState.getPosition() + 1));
            
            // Set background color based on state
            switch (questionState.getState()) {
                case QuestionState.STATE_ANSWERED:
                    questionNumber.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.success)));
                    break;
                case QuestionState.STATE_UNANSWERED:
                    questionNumber.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.error)));
                    break;
                case QuestionState.STATE_REVIEW:
                    questionNumber.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.accent_pink)));
                    break;
                default: // STATE_NOT_VISITED
                    questionNumber.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.gray)));
                    break;
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuestionClick(questionState.getPosition());
                }
            });
        }
    }
}
