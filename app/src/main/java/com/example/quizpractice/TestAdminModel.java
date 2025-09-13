package com.example.quizpractice;

public class TestAdminModel {
    private String docID;
    private String name;
    private int time; // in minutes
    private int questionCount;
    private String categoryID;

    public TestAdminModel() {
        // Required empty constructor for Firestore
    }

    public TestAdminModel(String docID, String name, int time, int questionCount, String categoryID) {
        this.docID = docID;
        this.name = name;
        this.time = time;
        this.questionCount = questionCount;
        this.categoryID = categoryID;
    }

    public String getDocID() {
        return docID;
    }
    
    public String getTestId() {
        return docID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public String getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(String categoryID) {
        this.categoryID = categoryID;
    }
}