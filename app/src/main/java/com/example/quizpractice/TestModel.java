package com.example.quizpractice;

public class TestModel {
    private String id;
    private String topScore;
    private int time;
    private String difficulty; // EASY, MEDIUM, HARD
    private int requiredScore; // Minimum score required to unlock this test
    private boolean isUnlocked; // Whether this test is accessible to the user

    public TestModel() {
        // Required empty constructor for Firebase
        this.id = "";
        this.topScore = "0";
        this.time = 0;
        this.difficulty = "EASY";
        this.requiredScore = 0;
        this.isUnlocked = true; // Easy tests are unlocked by default
    }

    public TestModel(String id, int topScore, int time) {
        this.id = id;
        this.topScore = String.valueOf(topScore);
        this.time = time;
        this.difficulty = "EASY";
        this.requiredScore = 0;
        this.isUnlocked = true;
    }

    public TestModel(String id, int topScore, int time, String difficulty, int requiredScore) {
        this.id = id;
        this.topScore = String.valueOf(topScore);
        this.time = time;
        this.difficulty = difficulty;
        this.requiredScore = requiredScore;
        
        // TestA is always unlocked by default
        this.isUnlocked = id.equals("A") || difficulty.equals("EASY");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopScore() {
        return topScore != null ? topScore : "0";
    }

    public void setTopScore(String topScore) {
        this.topScore = topScore;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getDifficulty() {
        return difficulty != null ? difficulty : "EASY";
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getRequiredScore() {
        return requiredScore;
    }

    public void setRequiredScore(int requiredScore) {
        this.requiredScore = requiredScore;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    // Helper method to get difficulty color
    public int getDifficultyColor() {
        switch (getDifficulty()) {
            case "EASY":
                return 0xFF4CAF50; // Green
            case "MEDIUM":
                return 0xFFFF9800; // Orange
            case "HARD":
                return 0xFFF44336; // Red
            default:
                return 0xFF4CAF50; // Default to green
        }
    }

    // Helper method to get difficulty text
    public String getDifficultyText() {
        return getDifficulty().toUpperCase();
    }
}
