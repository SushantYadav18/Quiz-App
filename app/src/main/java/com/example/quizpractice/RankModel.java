package com.example.quizpractice;

public class RankModel {
    private String userId;
    private String name;
    private String email;
    private int totalScore;
    private int rank;
    private long lastUpdated;

    public RankModel() {
        // Required empty constructor for Firestore
    }

    public RankModel(String userId, String name, String email, int totalScore, int rank, long lastUpdated) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.totalScore = totalScore;
        this.rank = rank;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
