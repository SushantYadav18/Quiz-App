package com.example.quizpractice;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
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

import com.example.quizpractice.databinding.ActivityMainBinding;
import com.example.quizpractice.CategoryFragment;
import com.example.quizpractice.LeaderBoardFragment;
import com.example.quizpractice.AccountFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout mainFrame;
    private Toolbar toolbar;

    private final BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = 
        item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                setFragment(new CategoryFragment());
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                setFragment(new LeaderBoardFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                setFragment(new AccountFragment());
                return true;
            }
            return false;
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize views
            mainFrame = findViewById(R.id.main_frame);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            toolbar = findViewById(R.id.toolbar);

            if (mainFrame == null || bottomNavigationView == null || toolbar == null) {
                Toast.makeText(this, "Error: Required views not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Set up toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            // Set up bottom navigation
            bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

            // Set default fragment
            setFragment(new CategoryFragment());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setFragment(Fragment fragment) {
        if (mainFrame != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frame, fragment);
            transaction.commit();
        }
    }
}