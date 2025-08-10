package com.example.quizpractice;

public class TestModel {
    private String id;
    private String topScore;
    private int time;

    public TestModel() {
        // Required empty constructor for Firebase
        this.id = "";
        this.topScore = "0";
        this.time = 0;
    }

    public TestModel(String id, int topScore, int time) {
        this.id = id;
        this.topScore = String.valueOf(topScore);
        this.time = time;
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
}
