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
    
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    
    public interface OnEditClickListener {
        void onEditClick(CategoryModel category, int position);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(CategoryModel category, int position);
    }
    
    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public CategoryAdapter(List<CategoryModel> catList, Context context) {
        this.catList = catList;
        this.context = context;
        Log.d(TAG, "Adapter created with " + (catList != null ? catList.size() : 0) + " items");
    }
    
    public CategoryAdapter(List<CategoryModel> catList) {
        this.catList = catList;
        Log.d(TAG, "Adapter created with " + (catList != null ? catList.size() : 0) + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        
        // Check if we're in admin mode (CategoryManagementActivity)
        boolean isAdminMode = context instanceof CategoryManagementActivity;
        int layoutId = isAdminMode ? R.layout.admin_cat_item_layout : R.layout.cat_item_layout;
        
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view, isAdminMode);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        try {
            if (catList != null && i < catList.size()) {
                CategoryModel model = catList.get(i);
                Log.d(TAG, "Binding category at position " + i + ": " + model.getName());
                
                holder.catName.setText(model.getName());
                holder.noOfTests.setText(model.getNoOfTests() + " Tests Available");

                if (!holder.isAdminMode) {
                    // User mode - setup navigation to tests
                    // Set click listener for the entire item
                    holder.itemView.setOnClickListener(v -> {
                        // Add click animation
                        v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .withEndAction(() -> {
                                        // Navigate to test selection
                                        Intent intent = new Intent(context, TestActivity.class);
                                        intent.putExtra("CAT_INDEX", i);
                                        context.startActivity(intent);
                                    })
                                    .start();
                            })
                            .start();
                    });

                    // Set click listener for start button
                    if (holder.startButton != null) {
                        holder.startButton.setOnClickListener(v -> {
                            // Add button click animation
                            v.animate()
                                .scaleX(0.9f)
                                .scaleY(0.9f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    v.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .withEndAction(() -> {
                                            // Navigate to test selection
                                            Intent intent = new Intent(context, TestActivity.class);
                                            intent.putExtra("CAT_INDEX", i);
                                            context.startActivity(intent);
                                        })
                                        .start();
                                })
                                .start();
                        });
                    }
                } else {
                    // Admin mode - setup edit and delete buttons
                    if (holder.editButton != null) {
                        holder.editButton.setOnClickListener(v -> {
                            if (onEditClickListener != null) {
                                onEditClickListener.onEditClick(model, i);
                            }
                        });
                    }
                    
                    if (holder.deleteButton != null) {
                        holder.deleteButton.setOnClickListener(v -> {
                            if (onDeleteClickListener != null) {
                                onDeleteClickListener.onDeleteClick(model, i);
                            }
                        });
                    }
                }
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView catName;
        TextView noOfTests;
        MaterialButton startButton;
        MaterialButton editButton;
        MaterialButton deleteButton;
        boolean isAdminMode;

        public ViewHolder(@NonNull View itemView, boolean isAdminMode) {
            super(itemView);
            this.isAdminMode = isAdminMode;
            
            catName = itemView.findViewById(R.id.catName);
            noOfTests = itemView.findViewById(R.id.noOfTests);
            
            if (isAdminMode) {
                // Admin mode - find edit and delete buttons
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                
                if (editButton != null && onEditClickListener != null) {
                    editButton.setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && onEditClickListener != null) {
                            onEditClickListener.onEditClick(catList.get(position), position);
                        }
                    });
                }
                
                if (deleteButton != null && onDeleteClickListener != null) {
                    deleteButton.setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && onDeleteClickListener != null) {
                            onDeleteClickListener.onDeleteClick(catList.get(position), position);
                        }
                    });
                }
            } else {
                // User mode - find start button
                startButton = itemView.findViewById(R.id.startButton);
                if (startButton == null) Log.e(TAG, "startButton not found");
            }
            
            if (catName == null) Log.e(TAG, "catName TextView not found");
            if (noOfTests == null) Log.e(TAG, "noOfTests TextView not found");
        }
    }
}

