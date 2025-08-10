package com.example.quizpractice;

import android.app.Application;
import android.util.Log;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class QuizApplication extends Application {
    private static final String TAG = "QuizApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            
            // Configure Firestore settings
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            
            // Check Google Play Services availability
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (googleAPI.isUserResolvableError(resultCode)) {
                    Log.w(TAG, "Google Play Services is not available but can be fixed");
                } else {
                    Log.e(TAG, "Google Play Services is not available on this device");
                }
            } else {
                Log.d(TAG, "Google Play Services is available");
            }
            
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
        }
    }
} 