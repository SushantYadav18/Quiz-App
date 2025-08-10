package com.example.quizpractice;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText name, email, password, confirmPassword;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private MaterialButton signupButton;
    private TextView loginText;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        name = findViewById(R.id.username);
        email = findViewById(R.id.emailid);
        password = findViewById(R.id.password1);
        confirmPassword = findViewById(R.id.password2);
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        signupButton = findViewById(R.id.signupB);
        loginText = findViewById(R.id.loginText);
        progressBar = findViewById(R.id.progressBar);

        signupButton.setOnClickListener(v -> {
            if (validateFields()) {
                signupNewUser();
            }
        });

        loginText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> onBackPressed());
    }

    private boolean validateFields() {
        String nameStr = name.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();
        String confirmPasswordStr = confirmPassword.getText().toString().trim();

        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (TextUtils.isEmpty(nameStr)) {
            nameLayout.setError("Name is required");
            name.requestFocus();
            return false;
        } else if (nameStr.length() < 3) {
            nameLayout.setError("Name must be at least 3 characters");
            name.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(emailStr)) {
            emailLayout.setError("Email is required");
            email.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            emailLayout.setError("Please enter a valid email");
            email.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordStr)) {
            passwordLayout.setError("Password is required");
            password.requestFocus();
            return false;
        } else if (passwordStr.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            password.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPasswordStr)) {
            confirmPasswordLayout.setError("Please confirm your password");
            confirmPassword.requestFocus();
            return false;
        } else if (!passwordStr.equals(confirmPasswordStr)) {
            confirmPasswordLayout.setError("Passwords do not match");
            confirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void signupNewUser() {
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                        DbQuery.createUserData(emailStr, name.getText().toString().trim() , new MyCompleteListener() {

                            @Override
                            public void onSuccess() {
                                DbQuery.loadData(new MyCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                        finish();
                                    }

                                    @Override
                                    public void onFailure() {
                                        Toast.makeText(SignUpActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();

                                    }
                                });

                            }

                            @Override
                            public void onFailure() {
                                Toast.makeText(SignUpActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();

                            }
                        });
                    } else {
                        String errorMessage = "Registration failed";
                        if (task.getException() instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                            switch (errorCode) {
                                case "ERROR_EMAIL_ALREADY_IN_USE":
                                    errorMessage = "Email is already registered";
                                    break;
                                case "ERROR_WEAK_PASSWORD":
                                    errorMessage = "Password is too weak";
                                    break;
                                case "ERROR_INVALID_EMAIL":
                                    errorMessage = "Invalid email format";
                                    break;
                            }
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        signupButton.setEnabled(!isLoading);
        name.setEnabled(!isLoading);
        email.setEnabled(!isLoading);
        password.setEnabled(!isLoading);
        confirmPassword.setEnabled(!isLoading);
        loginText.setEnabled(!isLoading);
    }
}

