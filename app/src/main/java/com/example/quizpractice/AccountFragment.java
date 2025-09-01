package com.example.quizpractice;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends Fragment {
    private Button logoutB;
    private TextView userNameText, userEmailText, sessionStartText, sessionDurationText, lastActivityText, expiryText;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize views
        logoutB = view.findViewById(R.id.LogoutB);
        userNameText = view.findViewById(R.id.userNameText);
        userEmailText = view.findViewById(R.id.userEmailText);
        sessionStartText = view.findViewById(R.id.sessionStartText);
        sessionDurationText = view.findViewById(R.id.sessionDurationText);
        lastActivityText = view.findViewById(R.id.lastActivityText);
        expiryText = view.findViewById(R.id.expiryText);

        // Load and display session information
        loadSessionInformation();


        logoutB.setOnClickListener(new View.OnClickListener() {

            @Override
            public  void onClick(View v) {

                // Use SessionManager to handle logout
                SessionManager sessionManager = SessionManager.getInstance(getActivity());
                sessionManager.logout();
                
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();
                
                // Sign out from Google
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
                googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(Task<Void> task) {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });

            }
        });

        return view;
    }
    
    /**
     * Load and display session information
     */
    private void loadSessionInformation() {
        SessionManager sessionManager = SessionManager.getInstance(getActivity());
        
        if (sessionManager.isLoggedIn()) {
            // Display user information
            userNameText.setText(sessionManager.getUserName() != null ? sessionManager.getUserName() : "Unknown");
            userEmailText.setText(sessionManager.getUserEmail() != null ? sessionManager.getUserEmail() : "Unknown");
            
            // Display session information
            updateSessionDisplay(sessionManager);
            
            // Update session information every 5 seconds
            startSessionUpdateTimer();
        } else {
            // User not logged in
            userNameText.setText("Not logged in");
            userEmailText.setText("Not logged in");
            sessionStartText.setText("N/A");
            sessionDurationText.setText("N/A");
            lastActivityText.setText("N/A");
            expiryText.setText("N/A");
        }
    }
    
    /**
     * Update session display with current information
     */
    private void updateSessionDisplay(SessionManager sessionManager) {
        SessionStats stats = sessionManager.getSessionStats();
        
        sessionStartText.setText(stats.getFormattedSessionStart());
        sessionDurationText.setText(stats.getFormattedSessionDuration());
        lastActivityText.setText(stats.getFormattedLastActivity());
        expiryText.setText(stats.getFormattedTimeUntilExpiry());
    }
    
    /**
     * Start timer to update session information periodically
     */
    private void startSessionUpdateTimer() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                SessionManager sessionManager = SessionManager.getInstance(getActivity());
                if (sessionManager.isLoggedIn()) {
                    updateSessionDisplay(sessionManager);
                    // Schedule next update in 5 seconds
                    handler.postDelayed(this, 5000);
                }
            }
        };
        
        // Start the first update
        handler.post(updateRunnable);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh session information when fragment becomes visible
        loadSessionInformation();
    }
}