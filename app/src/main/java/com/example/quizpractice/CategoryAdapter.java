package com.example.quizpractice;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private static final String TAG = "CategoryAdapter";
    private List<CategoryModel> catList;
    private Context context;

    public CategoryAdapter(List<CategoryModel> catList, Context context) {
        this.catList = catList;
        this.context = context;
        Log.d(TAG, "Adapter created with " + (catList != null ? catList.size() : 0) + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cat_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        try {
            if (catList != null && i < catList.size()) {
                CategoryModel model = catList.get(i);
                Log.d(TAG, "Binding category at position " + i + ": " + model.getName());
                
                holder.catName.setText(model.getName());
                holder.noOfTests.setText(model.getNoOfTests() + " Tests");

                holder.startButton.setOnClickListener(v -> {
                    Intent intent = new Intent(context, TestActivity.class);
                    intent.putExtra("CAT_INDEX", i);
                    context.startActivity(intent);
                });

                // Add click animation
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
                        })
                        .start();
                });
            } else {
                Log.e(TAG, "Invalid position or null list: position=" + i + ", list size=" + (catList != null ? catList.size() : 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        int count = catList != null ? catList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView catName;
        TextView noOfTests;
        MaterialButton startButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            catName = itemView.findViewById(R.id.catName);
            noOfTests = itemView.findViewById(R.id.noOfTests);
            startButton = itemView.findViewById(R.id.startButton);
            
            if (catName == null) Log.e(TAG, "catName TextView not found");
            if (noOfTests == null) Log.e(TAG, "noOfTests TextView not found");
            if (startButton == null) Log.e(TAG, "startButton not found");
        }
    }
}

