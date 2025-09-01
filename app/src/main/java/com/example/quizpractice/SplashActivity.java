package com.example.quizpractice;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds
    private TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appName = findViewById(R.id.App_name1);
        appName.setText(R.string.app_name);

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.myanim);
        appName.startAnimation(anim);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DbQuery.g_firestore = FirebaseFirestore.getInstance();
        
        // Check session validity using SessionManager
        SessionManager sessionManager = SessionManager.getInstance(this);
        
        if (mAuth.getCurrentUser() != null && sessionManager.isLoggedIn()) {
            // User is authenticated and has valid session
            DbQuery.loadData(new MyCompleteListener() {
                @Override
                public void onSuccess() {
                    // Update the drawer header with the loaded user data
                    MainActivity.updateDrawerHeader(SplashActivity.this);
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure() {
                    Toast.makeText(SplashActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                    // Redirect to login on failure
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            });
        } else {
            // No valid session, redirect to login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
