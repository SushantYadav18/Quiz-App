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

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<RankModel> rankList;
    private Context context;

    public LeaderboardAdapter(Context context, List<RankModel> rankList) {
        this.context = context;
        this.rankList = rankList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.leaderboard_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RankModel rankModel = rankList.get(position);
        holder.bind(rankModel, position + 1);
    }

    @Override
    public int getItemCount() {
        return rankList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView rankText, userNameText, userEmailText, scoreText;
        private View rankCircle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            userNameText = itemView.findViewById(R.id.userNameText);
            userEmailText = itemView.findViewById(R.id.userEmailText);
            scoreText = itemView.findViewById(R.id.scoreText);
            rankCircle = itemView.findViewById(R.id.rankCircle);
        }

        public void bind(RankModel rankModel, int rank) {
            // Set rank
            rankText.setText(String.valueOf(rank));

            // Set user info
            userNameText.setText(rankModel.getName());
            userEmailText.setText(rankModel.getEmail());

            // Set score
            scoreText.setText(String.valueOf(rankModel.getTotalScore()));

            // Set rank circle color based on position
            setRankCircleColor(rank);

            // Highlight current user
            if (isCurrentUser(rankModel.getUserId())) {
                highlightCurrentUser();
            }
        }

        private void setRankCircleColor(int rank) {
            int color;
            switch (rank) {
                case 1:
                    color = Color.parseColor("#FFD700"); // Gold
                    break;
                case 2:
                    color = Color.parseColor("#C0C0C0"); // Silver
                    break;
                case 3:
                    color = Color.parseColor("#CD7F32"); // Bronze
                    break;
                default:
                    color = context.getResources().getColor(R.color.primary);
                    break;
            }
            rankCircle.setBackgroundColor(color);
        }

        private boolean isCurrentUser(String userId) {
            // Check if this is the current user
            return userId.equals(DbQuery.myProfile.getUserId());
        }

        private void highlightCurrentUser() {
            // Highlight current user's row
            itemView.setBackgroundColor(context.getResources().getColor(R.color.background_light));
            itemView.setElevation(8f);
        }
    }
}
