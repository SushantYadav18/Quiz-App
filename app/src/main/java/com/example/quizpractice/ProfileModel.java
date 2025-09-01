package com.example.quizpractice;

public class ProfileModel {
    private String userId;
    private String name;
    private String email;
    private String profile;

    public ProfileModel() {
        // Required empty constructor for Firestore
    }

    public ProfileModel(String userId, String name, String email, String profile) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profile = profile;
    }

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

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
