package com.example.quizpractice;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
                
                holder.testNo.setText("Test No. " + model.getId());
                holder.topScore.setText(model.getTopScore() + "%");
                holder.progressBar.setMax(100);
                holder.progressBar.setProgress(Integer.parseInt(model.getTopScore()));

                holder.testNo.setVisibility(View.VISIBLE);
                holder.topScore.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.itemView.setVisibility(View.VISIBLE);

                Log.d(TAG, String.format("Item %d dimensions - Width: %d, Height: %d", 
                    position, holder.itemView.getWidth(), holder.itemView.getHeight()));

                holder.itemView.setOnClickListener(v -> {
                    Toast.makeText(context, "Test " + model.getId() + " selected", Toast.LENGTH_SHORT).show();
                });
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
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            testNo = itemView.findViewById(R.id.testNum);
            topScore = itemView.findViewById(R.id.Scoretext);
            progressBar = itemView.findViewById(R.id.testprogressBar);

            if (testNo == null) Log.e("ViewHolder", "testNo TextView not found");
            if (topScore == null) Log.e("ViewHolder", "topScore TextView not found");
            if (progressBar == null) Log.e("ViewHolder", "progressBar not found");
        }
    }
}
