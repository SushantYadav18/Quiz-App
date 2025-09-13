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
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001; // Request code for Google Sign-In
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
        // Check Google Play Services availability before setup
        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play Services not available, disabling Google Sign-In");
            findViewById(R.id.Gsign_in_button).setEnabled(false);
            findViewById(R.id.Gsign_in_button).setAlpha(0.5f);
            Toast.makeText(this, "Google Sign-In requires Google Play Services", Toast.LENGTH_LONG).show();
        } else {
            setupGoogleSignIn();
        }
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
        MaterialButton adminLoginButton = binding.adminLoginBtn;

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
        
        adminLoginButton.setOnClickListener(v -> {
            Log.d(TAG, "Admin Login button clicked");
            startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class));
        });

        // Initialize signInLauncher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Google Sign-In activity result received, result code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Google Sign-In result OK");
                        if (result.getData() == null) {
                            Log.e(TAG, "Google Sign-In failed: Intent data is null");
                            Toast.makeText(this, "Google Sign-In failed: No data received", Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            return;
                        }
                        
                        try {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            firebaseAuthWithGoogle(task);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Google Sign-In result", e);
                            Toast.makeText(this, "Google Sign-In processing error: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            setLoading(false);
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Log.d(TAG, "Google Sign-In was canceled by user");
                        Toast.makeText(this, "Sign-in canceled", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    } else {
                        Log.e(TAG, "Google Sign-In failed: Result code = " + result.getResultCode());
                        Toast.makeText(this, "Google Sign-In failed with code: " + result.getResultCode(), 
                                Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                }
        );
    }

    private void setupGoogleSignIn() {
        try {
            String webClientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Setting up Google Sign-In with web client ID: " + webClientId);
            
            if (webClientId == null || webClientId.isEmpty() || webClientId.equals("default_web_client_id")) {
                Log.e(TAG, "Web client ID is null, empty, or not properly configured!");
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
            
            // Check for existing Google Sign In account
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                Log.d(TAG, "Found existing Google Sign-In account: " + account.getEmail() + ", ID token available: " + (account.getIdToken() != null));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Google Sign-In", e);
            Toast.makeText(this, "Failed to setup Google Sign-In: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void googleSignIn() {
        Log.d(TAG, "googleSignIn() method called");
        setLoading(true);
        
        if (mGoogleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient is null! Re-initializing...");
            setupGoogleSignIn();
            if (mGoogleSignInClient == null) {
                Log.e(TAG, "Failed to create GoogleSignInClient after retry");
                Toast.makeText(this, "Google Sign-In not available. Please check your configuration.", 
                        Toast.LENGTH_LONG).show();
                setLoading(false);
                return;
            }
        }
        
        // Check Google Play Services availability
        if (!isGooglePlayServicesAvailable()) {
            setLoading(false);
            return;
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
            setLoading(false);
        }
    }
    
    /**
     * Checks if Google Play Services is available and up to date
     * @return true if Google Play Services is available and up to date
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services is not available: " + resultCode);
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, RC_SIGN_IN,
                        dialog -> {
                            Log.d(TAG, "Google Play Services error dialog dismissed");
                            setLoading(false);
                        }).show();
            } else {
                Log.e(TAG, "This device does not support Google Play Services: " + resultCode);
                Toast.makeText(this, "This device does not support Google Play Services", 
                        Toast.LENGTH_LONG).show();
                setLoading(false);
            }
            return false;
        }
        Log.d(TAG, "Google Play Services is available and up to date");
        return true;
    }

    private void firebaseAuthWithGoogle(Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "Starting Firebase authentication with Google");
        setLoading(true);
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail() + ", ID token available: " + (account.getIdToken() != null));
                
                if (account.getIdToken() == null) {
                    Log.e(TAG, "ID token is null, cannot authenticate with Firebase");
                    Toast.makeText(LoginActivity.this, "Authentication failed: ID token is null", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    return;
                }
                
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                Log.d(TAG, "Created AuthCredential, attempting Firebase sign-in");
                
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            setLoading(false);
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase authentication successful");
                                Toast.makeText(LoginActivity.this, "Google sign-in successful!", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = mAuth.getCurrentUser();
                                
                                if (user == null) {
                                    Log.e(TAG, "Firebase user is null after successful authentication");
                                    Toast.makeText(LoginActivity.this, "Authentication error: User is null", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if(task.getResult().getAdditionalUserInfo().isNewUser()){
                                    Log.d(TAG, "New user, creating user data for: " + user.getEmail());
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
                                            
                                            // Set current user in UserProgressManager
                                            UserProgressManager.getInstance(LoginActivity.this).setCurrentUser(user.getUid());
                                            
                                            DbQuery.loadData(new MyCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "Data loaded successfully");
                                                    
                                                    // Load user progress data from Firebase
                                                    UserProgressManager.getInstance(LoginActivity.this).loadUserProgressFromFirebase(new MyCompleteListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Log.d(TAG, "User progress loaded successfully, starting MainActivity");
                                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                            finish();
                                                        }
                                                        
                                                        @Override
                                                        public void onFailure() {
                                                            Log.e(TAG, "Failed to load user progress data");
                                                            Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
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
                                        
                                        // Set current user in UserProgressManager
                                        UserProgressManager.getInstance(LoginActivity.this).setCurrentUser(existingUser.getUid());
                                        
                                        Log.d(TAG, "Google Sign-In session created successfully for existing user: " + existingUser.getEmail());
                                    }
                                    
                                    DbQuery.loadData(new MyCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Data loaded successfully");
                                            
                                            // Load user progress data from Firebase
                                            UserProgressManager.getInstance(LoginActivity.this).loadUserProgressFromFirebase(new MyCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "User progress loaded successfully, starting MainActivity");
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                }
                                                
                                                @Override
                                                public void onFailure() {
                                                    Log.e(TAG, "Failed to load user progress data");
                                                    Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure() {
                                            Log.e(TAG, "Failed to load data");
                                            Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            } else {
                                Exception exception = task.getException();
                                Log.e(TAG, "Firebase authentication failed", exception);
                                
                                String errorMessage = "Authentication failed";
                                if (exception != null) {
                                    String exceptionMessage = exception.getMessage();
                                    Log.e(TAG, "Exception message: " + exceptionMessage);
                                    
                                    // Check for common Firebase Auth errors
                                    if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                        errorMessage = "Invalid credentials";
                                    } else if (exception instanceof FirebaseAuthInvalidUserException) {
                                        errorMessage = "Account doesn't exist or has been disabled";
                                    } else if (exceptionMessage != null && exceptionMessage.contains("network")) {
                                        errorMessage = "Network error, please check your connection";
                                    } else if (exceptionMessage != null) {
                                        errorMessage = exceptionMessage;
                                    }
                                }
                                
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Log.e(TAG, "Google Sign-In account is null");
                setLoading(false);
            }
        } catch (ApiException e) {
            setLoading(false);
            int statusCode = e.getStatusCode();
            String errorMessage;
            
            switch (statusCode) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    errorMessage = "Sign-in was cancelled";
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                    errorMessage = "Sign-in failed";
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                    errorMessage = "Sign-in is already in progress";
                    break;
                case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                    errorMessage = "Invalid account";
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                    errorMessage = "Sign-in required";
                    break;
                case GoogleSignInStatusCodes.NETWORK_ERROR:
                    errorMessage = "Network error occurred";
                    break;
                case GoogleSignInStatusCodes.INTERNAL_ERROR:
                    errorMessage = "Internal error occurred";
                    break;
                default:
                    errorMessage = "Error code: " + statusCode;
            }
            
            Log.e(TAG, "Google Sign-In failed: " + errorMessage, e);
            Toast.makeText(this, "Google Sign-In failed: " + errorMessage, Toast.LENGTH_SHORT).show();
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
                            
                            // Set current user in UserProgressManager
                            UserProgressManager.getInstance(LoginActivity.this).setCurrentUser(user.getUid());
                            
                            Log.d(TAG, "User session created successfully for: " + email);
                        }
                        
                        DbQuery.loadData(new MyCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Data loaded successfully");
                                
                                // Load user progress data from Firebase
                                UserProgressManager.getInstance(LoginActivity.this).loadUserProgressFromFirebase(new MyCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "User progress loaded successfully, starting MainActivity");
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }
                                    
                                    @Override
                                    public void onFailure() {
                                        Log.e(TAG, "Failed to load user progress data");
                                        Toast.makeText(LoginActivity.this, "Something went Wrong !Please Try Again  Later !!", Toast.LENGTH_SHORT).show();
                                    }
                                });
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
