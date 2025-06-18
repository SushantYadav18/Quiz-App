package com.example.quizpractice;

public class TestModel {
    private String id;
    private String topScore;
    private int time;

    public TestModel() {
        // Required empty constructor for Firebase
    }

    public TestModel(String id, String topScore, int time) {
        this.id = id;
        this.topScore = topScore;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopScore() {
        return topScore;
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
