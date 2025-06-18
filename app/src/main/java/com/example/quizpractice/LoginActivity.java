package com.example.quizpractice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizpractice.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SignInClient oneTapClient;
    private ActivityResultLauncher<IntentSenderRequest> signInLauncher;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Google Sign-In
        oneTapClient = Identity.getSignInClient(this);
        setupGoogleSignIn();

        // Initialize views
        progressBar = binding.progressBar;
        TextInputLayout emailLayout = binding.email;
        TextInputLayout passwordLayout = binding.passwordLayout;
        TextInputEditText emailEdit = binding.emailid;
        TextInputEditText passwordEdit = binding.password;
        MaterialButton loginButton = binding.loginB;
        MaterialButton signupButton = binding.signupB;
        View googleSignInButton = binding.GsignInButton;

        // Set click listeners
        loginButton.setOnClickListener(v -> {
            if (validateFields()) {
                loginUser(emailEdit.getText().toString().trim(),
                        passwordEdit.getText().toString().trim());
            }
        });

        signupButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        googleSignInButton.setOnClickListener(v -> googleSignIn());
    }

    private void setupGoogleSignIn() {
        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                    String idToken = credential.getGoogleIdToken();
                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken);
                    }
                } catch (ApiException e) {
                    Log.w(TAG, "Google sign in failed", e);
                    Toast.makeText(this, "Google sign in failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void googleSignIn() {
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                                result.getPendingIntent().getIntentSender()).build();
                        signInLauncher.launch(intentSenderRequest);
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Google sign in failed", e);
                    Toast.makeText(this, "Google sign in failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {

                        Toast.makeText(LoginActivity.this, "Google sign-in successful!", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = mAuth.getCurrentUser();

                        if(task.getResult().getAdditionalUserInfo().isNewUser()){
                            DbQuery.createUserData(user.getEmail(), user.getDisplayName(), new MyCompleteListener() {
                                @Override
                                public void onSuccess() {

                                    DbQuery.loadCategories(new MyCompleteListener() {
                                        @Override
                                        public void onSuccess() {

                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }

                                        @Override
                                        public void onFailure() {
                                            Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onFailure() {

                                    Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else {
                            DbQuery.loadCategories(new MyCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onFailure() {
                                    Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(FirebaseUser user) {
        UserData userData = new UserData(user.getDisplayName(), user.getEmail());
        mDatabase.child("users").child(user.getUid()).setValue(userData);
    }

    private boolean validateFields() {
        TextInputLayout emailLayout = binding.email;
        TextInputLayout passwordLayout = binding.passwordLayout;
        TextInputEditText emailEdit = binding.emailid;
        TextInputEditText passwordEdit = binding.password;

        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        // Reset errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Validate email
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            emailEdit.requestFocus();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordEdit.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful!",
                                Toast.LENGTH_SHORT).show();
                       DbQuery.loadCategories(new MyCompleteListener() {
                           @Override
                           public void onSuccess() {

                               startActivity(new Intent(LoginActivity.this, MainActivity.class));
                               finish();
                           }

                           @Override
                           public void onFailure() {

                               Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                           }
                       });



                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginB.setEnabled(!isLoading);
        binding.signupB.setEnabled(!isLoading);
        binding.GsignInButton.setEnabled(!isLoading);
        binding.emailid.setEnabled(!isLoading);
        binding.password.setEnabled(!isLoading);
    }
}
