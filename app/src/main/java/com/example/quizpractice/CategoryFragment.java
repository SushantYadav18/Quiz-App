package com.example.quizpractice;

import static com.example.quizpractice.DbQuery.g_catList;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CategoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CategoryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "CategoryFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private RecyclerView catRecycler;
    private CategoryAdapter adapter;

    public CategoryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CategoryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CategoryFragment newInstance(String param1, String param2) {
        CategoryFragment fragment = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        // Initialize views
        catRecycler = view.findViewById(R.id.cat_grid);
        TextView titleView = view.findViewById(R.id.categoryTitle);
        TextView subtitleView = view.findViewById(R.id.categorySubtitle);

        // Setup RecyclerView with proper scrolling
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        catRecycler.setLayoutManager(layoutManager);
        catRecycler.setHasFixedSize(true);
        catRecycler.setNestedScrollingEnabled(true);
        catRecycler.setScrollContainer(true);

        // Setup adapter FIRST
        adapter = new CategoryAdapter(g_catList, requireContext());
        catRecycler.setAdapter(adapter);

        // Add scroll listener for debugging
        catRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d(TAG, "Scrolled: dx=" + dx + ", dy=" + dy);
            }
        });

        // Now load categories from database
        loadCategoriesFromDatabase();

        return view;
    }

    private void loadCategoriesFromDatabase() {
        Log.d(TAG, "Loading categories from database...");
        
        DbQuery.loadCategories(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Categories loaded successfully. Count: " + g_catList.size());
                
                // Add sample categories for testing if database is empty
                if (g_catList.isEmpty()) {
                    addSampleCategories();
                }
                
                adapter.notifyDataSetChanged();
                updateStatsCards();
                
                if (g_catList.isEmpty()) {
                    // Show a message if no categories found
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No categories found in database", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show success message
                    if (getContext() != null) {
                        Toast.makeText(getContext(), g_catList.size() + " categories loaded", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load categories from database");
                // Add sample categories for testing
                addSampleCategories();
                adapter.notifyDataSetChanged();
                updateStatsCards();
                
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Added sample categories for testing", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addSampleCategories() {
        // Add sample categories for testing scrolling
        g_catList.add(new CategoryModel("cat1", "Mathematics", 15));
        g_catList.add(new CategoryModel("cat2", "Science", 12));
        g_catList.add(new CategoryModel("cat3", "History", 8));
        g_catList.add(new CategoryModel("cat4", "Geography", 10));
        g_catList.add(new CategoryModel("cat5", "Literature", 6));
        g_catList.add(new CategoryModel("cat6", "Computer Science", 20));
        g_catList.add(new CategoryModel("cat7", "Art & Music", 5));
        g_catList.add(new CategoryModel("cat8", "Sports", 7));
        g_catList.add(new CategoryModel("cat9", "Politics", 9));
        g_catList.add(new CategoryModel("cat10", "Economics", 11));
        g_catList.add(new CategoryModel("cat11", "Psychology", 8));
        g_catList.add(new CategoryModel("cat12", "Philosophy", 6));
        
        Log.d(TAG, "Added " + g_catList.size() + " sample categories for testing");
    }

    private void updateStatsCards() {
        // Update the stats cards in the layout
        View view = getView();
        if (view != null) {
            TextView categoriesCount = view.findViewById(R.id.categoriesCount);
            TextView completedCount = view.findViewById(R.id.completedCount);
            
            if (categoriesCount != null) {
                categoriesCount.setText(String.valueOf(g_catList.size()));
            }
            
            if (completedCount != null) {
                completedCount.setText("0"); // You can update this with actual completed count
            }
        }
    }
}