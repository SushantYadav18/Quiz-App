package com.example.quizpractice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.example.quizpractice.databinding.ActivityMainBinding;
import com.example.quizpractice.CategoryFragment;
import com.example.quizpractice.LeaderBoardFragment;
import com.example.quizpractice.AccountFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout mainFrame;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private static TextView drawerProfileName, drawerProfileText;

    private final BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = 
        item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                setFragment(new CategoryFragment());
                updateToolbarTitle("Quiz Categories");
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                setFragment(new LeaderBoardFragment());
                updateToolbarTitle("Leaderboard");
                return true;
            } else if (itemId == R.id.nav_profile) {
                setFragment(new AccountFragment());
                updateToolbarTitle("Profile");
                return true;
            }
            return false;
        };

    // Call this after user data is loaded to update the drawer header
    public static void updateDrawerHeader(Context context) {
        NavigationView navigationView = ((AppCompatActivity) context).findViewById(R.id.nav_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            drawerProfileName = headerView.findViewById(R.id.nav_drawer_name);
            drawerProfileText = headerView.findViewById(R.id.nav_drawer_text_img);

            String name = DbQuery.myProfile != null && DbQuery.myProfile.getName() != null ? DbQuery.myProfile.getName() : "User";
            drawerProfileName.setText(name);
            if (!name.isEmpty()) {
                drawerProfileText.setText(name.substring(0, 1).toUpperCase());
            } else {
                drawerProfileText.setText("U");
            }
        }
    }

    private void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
    
    /**
     * Redirect user to login activity when session is invalid
     */
    private void redirectToLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
    
    /**
     * Handle session logout
     */
    public void logout() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        sessionManager.logout();
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Redirect to login
        redirectToLogin();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Update last activity when app goes to background
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            sessionManager.updateLastActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Check session validity
            SessionManager sessionManager = SessionManager.getInstance(this);
            if (!sessionManager.isLoggedIn()) {
                Log.w(TAG, "No valid session found, redirecting to login");
                redirectToLogin();
                return;
            }
            
            // Refresh session on activity start
            sessionManager.refreshSession();
            Log.d(TAG, "Session refreshed for user: " + sessionManager.getUserEmail());

            // Initialize views
            mainFrame = findViewById(R.id.main_frame);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            toolbar = findViewById(R.id.toolbar);
            drawerLayout = findViewById(R.id.drawer_layout);

            if (mainFrame == null || bottomNavigationView == null || toolbar == null || drawerLayout == null) {
                Toast.makeText(this, "Error: Required views not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Set up toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                getSupportActionBar().setTitle("Quiz App");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_camera);
            }

            // Set up Drawer Toggle (hamburger icon)
            drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();

            // Set up bottom navigation
            bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

            // Set up Navigation Drawer profile info (will update after user data is loaded)
            updateDrawerHeader(this);

            // Handle navigation item clicks
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(item -> {
                    int id = item.getItemId();
                    // Handle navigation item clicks here
                    if (id == R.id.nav_home) {
                        setFragment(new CategoryFragment());
                        updateToolbarTitle("Quiz Categories");
                    } else if (id == R.id.nav_categories) {
                        setFragment(new CategoryFragment());
                        updateToolbarTitle("Categories");
                    } else if (id == R.id.nav_leaderboard) {
                        setFragment(new LeaderBoardFragment());
                        updateToolbarTitle("Leaderboard");
                    } else if (id == R.id.nav_profile) {
                        setFragment(new AccountFragment());
                        updateToolbarTitle("Profile");
                    } else if (id == R.id.nav_settings) {
                        // Handle settings
                        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                    } else if (id == R.id.nav_help) {
                        // Handle help
                        Toast.makeText(this, "Help & Support", Toast.LENGTH_SHORT).show();
                    } else if (id == R.id.nav_about) {
                        // Handle about
                        Toast.makeText(this, "About", Toast.LENGTH_SHORT).show();
                    }
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                });
            }

            // Set default fragment
            setFragment(new CategoryFragment());
            updateToolbarTitle("Quiz Categories");
            
            // Check if we should open leaderboard directly
            Intent intent = getIntent();
            if (intent != null && intent.getBooleanExtra("OPEN_LEADERBOARD", false)) {
                // Navigate directly to leaderboard
                setFragment(new LeaderBoardFragment());
                updateToolbarTitle("Leaderboard");
                // Update bottom navigation to show leaderboard as selected
                bottomNavigationView.setSelectedItemId(R.id.nav_leaderboard);
                Log.d(TAG, "Opened leaderboard directly from ResultActivity");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void reloadUserDataAndHeader() {
        DbQuery.getUserData(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                updateDrawerHeader(MainActivity.this);
            }
            @Override
            public void onFailure() {
                // Optionally show an error or fallback
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Refresh session on resume
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            sessionManager.refreshSession();
        }
        
        // Reload user data and header
        reloadUserDataAndHeader();
    }

    private void setFragment(Fragment fragment) {
        if (mainFrame != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frame, fragment);
            transaction.commit();
        }
    }
}