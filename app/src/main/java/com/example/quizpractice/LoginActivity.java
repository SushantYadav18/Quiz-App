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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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
    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Log Firebase configuration
        Log.d(TAG, "Firebase Auth initialized: " + (mAuth != null));
        Log.d(TAG, "Firebase Database initialized: " + (mDatabase != null));

        // Initialize Google Sign-In
        Log.d(TAG, "About to setup Google Sign-In");
        setupGoogleSignIn();
        Log.d(TAG, "Google Sign-In setup completed, client: " + (mGoogleSignInClient != null));

        // Initialize views
        progressBar = binding.progressBar;
        TextInputLayout emailLayout = binding.email;
        TextInputLayout passwordLayout = binding.passwordLayout;
        TextInputEditText emailEdit = binding.emailid;
        TextInputEditText passwordEdit = binding.password;
        MaterialButton loginButton = binding.loginB;
        MaterialButton signupButton = binding.signupB;
        View googleSignInButton = findViewById(R.id.Gsign_in_button);

        // Log view initialization
        Log.d(TAG, "Google Sign-In button found: " + (googleSignInButton != null));

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

        googleSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Google Sign-In button clicked");
            googleSignIn();
        });

        // Initialize signInLauncher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ActivityResultLauncher triggered with result code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Result OK, processing Google Sign-In result");
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        firebaseAuthWithGoogle(task);
                    } else {
                        Log.e(TAG, "Google Sign-In result not OK: " + result.getResultCode());
                        if (result.getData() != null) {
                            Log.d(TAG, "Result data: " + result.getData().toString());
                        }
                    }
                }
        );
    }

    private void setupGoogleSignIn() {
        try {
            String webClientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Setting up Google Sign-In with web client ID: " + webClientId);
            
            if (webClientId == null || webClientId.isEmpty()) {
                Log.e(TAG, "Web client ID is null or empty!");
                Toast.makeText(this, "Google Sign-In configuration error: Missing web client ID", 
                        Toast.LENGTH_LONG).show();
                return;
            }
            
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .requestProfile()
                    .build();

            Log.d(TAG, "GoogleSignInOptions built successfully");
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.d(TAG, "Google Sign-In client created successfully: " + (mGoogleSignInClient != null));
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Google Sign-In", e);
            Toast.makeText(this, "Failed to setup Google Sign-In: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void googleSignIn() {
        Log.d(TAG, "googleSignIn() method called");
        
        if (mGoogleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient is null! Re-initializing...");
            setupGoogleSignIn();
            if (mGoogleSignInClient == null) {
                Log.e(TAG, "Failed to create GoogleSignInClient after retry");
                Toast.makeText(this, "Google Sign-In not available. Please check your configuration.", 
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        try {
            Log.d(TAG, "Getting sign-in intent from GoogleSignInClient");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            Log.d(TAG, "Sign-in intent created successfully, launching...");
            signInLauncher.launch(signInIntent);
            Log.d(TAG, "Sign-in intent launched successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error launching Google Sign-In", e);
            Toast.makeText(this, "Google Sign-In not available: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "Starting Firebase authentication with Google");
        setLoading(true);
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            setLoading(false);
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase authentication successful");
                                Toast.makeText(LoginActivity.this, "Google sign-in successful!", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = mAuth.getCurrentUser();

                                if(task.getResult().getAdditionalUserInfo().isNewUser()){
                                    Log.d(TAG, "New user, creating user data");
                                    DbQuery.createUserData(user.getEmail(), user.getDisplayName(), new MyCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "User data created successfully");
                                            
                                            // Create user session for new Google Sign-In user
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                SessionManager sessionManager = SessionManager.getInstance(LoginActivity.this);
                                                sessionManager.createSession(user.getUid(), user.getEmail(), user.getDisplayName());
                                                
                                                Log.d(TAG, "Google Sign-In session created successfully for new user: " + user.getEmail());
                                            }
                                            
                                            DbQuery.loadData(new MyCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "Data loaded successfully, starting MainActivity");
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                }

                                                @Override
                                                public void onFailure() {
                                                    Log.e(TAG, "Failed to load data");
                                                    Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure() {
                                            Log.e(TAG, "Failed to create user data");
                                            Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                else {
                                    Log.d(TAG, "Existing user, loading data");
                                    
                                    // Create user session for existing Google Sign-In user
                                    FirebaseUser existingUser = mAuth.getCurrentUser();
                                    if (existingUser != null) {
                                        SessionManager sessionManager = SessionManager.getInstance(LoginActivity.this);
                                        sessionManager.createSession(existingUser.getUid(), existingUser.getEmail(), existingUser.getDisplayName());
                                        
                                        Log.d(TAG, "Google Sign-In session created successfully for existing user: " + existingUser.getEmail());
                                    }
                                    
                                    DbQuery.loadData(new MyCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Data loaded successfully, starting MainActivity");
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }

                                        @Override
                                        public void onFailure() {
                                            Log.e(TAG, "Failed to load data");
                                            Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            } else {
                                Log.e(TAG, "Firebase authentication failed", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                        task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Log.e(TAG, "Google Sign-In account is null");
                setLoading(false);
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed with API exception", e);
            setLoading(false);
            Toast.makeText(this, "Google sign in failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
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
                        
                        // Create user session
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            SessionManager sessionManager = SessionManager.getInstance(this);
                            sessionManager.createSession(user.getUid(), email, user.getDisplayName() != null ? user.getDisplayName() : "User");
                            
                            Log.d(TAG, "User session created successfully for: " + email);
                        }
                        
                        DbQuery.loadData(new MyCompleteListener() {
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
        findViewById(R.id.Gsign_in_button).setEnabled(!isLoading);
        binding.emailid.setEnabled(!isLoading);
        binding.password.setEnabled(!isLoading);
    }
}
