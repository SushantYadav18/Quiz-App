package com.example.quizpractice;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderBoardFragment extends Fragment {

    private static final String TAG = "LeaderBoardFragment";

    private RecyclerView leaderboardRecycler;
    private ProgressBar progressBar;
    private TextView noDataText;
    private TextView currentUserRankText;
    private TextView currentUserScoreText;

    private LeaderboardAdapter adapter;
    private List<RankModel> rankList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leader_board, container, false);

        initViews(view);
        setupRecyclerView();
        loadLeaderboardData();

        return view;
    }

    private void initViews(View view) {
        leaderboardRecycler = view.findViewById(R.id.leaderboardRecycler);
        progressBar = view.findViewById(R.id.progressBar);
        noDataText = view.findViewById(R.id.noDataText);
        currentUserRankText = view.findViewById(R.id.currentUserRankText);
        currentUserScoreText = view.findViewById(R.id.currentUserScoreText);
    }

    private void setupRecyclerView() {
        rankList = new ArrayList<>();
        adapter = new LeaderboardAdapter(requireContext(), rankList);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        leaderboardRecycler.setLayoutManager(layoutManager);
        leaderboardRecycler.setAdapter(adapter);
        leaderboardRecycler.setHasFixedSize(true);
    }

    private void loadLeaderboardData() {
        showLoading(true);
        
        // Load leaderboard data from database
        DbQuery.loadLeaderboardData(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                displayLeaderboard();
            }

            @Override
            public void onFailure() {
                showLoading(false);
                showError("Failed to load leaderboard data");
            }
        });
    }

    private void displayLeaderboard() {
        if (DbQuery.g_rankList != null && !DbQuery.g_rankList.isEmpty()) {
            // Sort by score (highest first)
            Collections.sort(DbQuery.g_rankList, (r1, r2) -> 
                Integer.compare(r2.getTotalScore(), r1.getTotalScore()));
            
            // Update adapter
            rankList.clear();
            rankList.addAll(DbQuery.g_rankList);
            adapter.notifyDataSetChanged();
            
            // Show current user's rank and score
            showCurrentUserInfo();
            
            // Hide no data text
            noDataText.setVisibility(View.GONE);
            leaderboardRecycler.setVisibility(View.VISIBLE);
            
            Log.d(TAG, "Leaderboard displayed with " + rankList.size() + " users");
        } else {
            showNoData();
        }
    }

    private void showCurrentUserInfo() {
        if (DbQuery.myProfile != null && DbQuery.myProfile.getUserId() != null) {
            String currentUserId = DbQuery.myProfile.getUserId();
            
            // Find current user's rank
            int currentUserRank = -1;
            int currentUserScore = 0;
            
            for (int i = 0; i < rankList.size(); i++) {
                RankModel rank = rankList.get(i);
                if (rank.getUserId().equals(currentUserId)) {
                    currentUserRank = i + 1;
                    currentUserScore = rank.getTotalScore();
                    break;
                }
            }
            
            if (currentUserRank > 0) {
                currentUserRankText.setText("Your Rank: #" + currentUserRank);
                currentUserScoreText.setText("Your Score: " + currentUserScore);
                
                currentUserRankText.setVisibility(View.VISIBLE);
                currentUserScoreText.setVisibility(View.VISIBLE);
            } else {
                currentUserRankText.setVisibility(View.GONE);
                currentUserScoreText.setVisibility(View.GONE);
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        leaderboardRecycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showNoData() {
        noDataText.setVisibility(View.VISIBLE);
        leaderboardRecycler.setVisibility(View.GONE);
        currentUserRankText.setVisibility(View.GONE);
        currentUserScoreText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        showNoData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (rankList.isEmpty()) {
            loadLeaderboardData();
        }
    }
}