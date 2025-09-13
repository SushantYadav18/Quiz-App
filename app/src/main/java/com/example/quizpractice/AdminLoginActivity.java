package com.example.quizpractice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameLayout, passwordLayout;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private ImageView backButton;

    // Admin credentials
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize views
        usernameEditText = findViewById(R.id.adminUsername);
        passwordEditText = findViewById(R.id.adminPassword);
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.adminPasswordLayout);
        loginButton = findViewById(R.id.adminLoginButton);
        progressBar = findViewById(R.id.adminProgressBar);
        backButton = findViewById(R.id.backBtn);

        // Set click listeners
        loginButton.setOnClickListener(v -> {
            if (validateFields()) {
                loginAdmin();
            }
        });

        backButton.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
    }

    private boolean validateFields() {
        boolean isValid = true;

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    private void loginAdmin() {
        setLoading(true);

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            // Check admin credentials
            if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
                // Login successful
                Toast.makeText(AdminLoginActivity.this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                
                // Create admin session
                SessionManager sessionManager = SessionManager.getInstance(AdminLoginActivity.this);
                sessionManager.createAdminSession();
                
                // Navigate to admin dashboard
                Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Login failed
                Toast.makeText(AdminLoginActivity.this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
            }
            setLoading(false);
        }, 1000); // Delay for 1 second to simulate network request
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        usernameEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
    }
}