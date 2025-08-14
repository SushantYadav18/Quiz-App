package com.example.quizpractice;

import static com.example.quizpractice.DbQuery.loadquestions;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class test_startActivity extends AppCompatActivity {

    private TextView catName, testNo, totalQues, bestScore, time;
    private Button startBtn;
    private ImageView backBtn;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test_start);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        init();
        loadingDialog.show();
        
        // Debug database structure first
        DbQuery.debugDatabaseStructure(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                Log.d("test_startActivity", "Database structure debug completed");
                // Now load questions
                loadquestions(new MyCompleteListener(){

                    @Override
                    public void onSuccess() {
                        setData();
                        loadingDialog.dismiss();
                    }

                    @Override
                    public void onFailure() {
                        loadingDialog.dismiss();
                        Toast.makeText(test_startActivity.this,
                                "Failed to load tests. Please try again later.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure() {
                Log.e("test_startActivity", "Database structure debug failed");
                loadingDialog.dismiss();
                Toast.makeText(test_startActivity.this,
                        "Failed to access database. Please check your connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

        private void init() {
            catName = findViewById(R.id.st_cty);
            testNo = findViewById(R.id.st_testNo);
            totalQues = findViewById(R.id.st_totalQues);
            bestScore = findViewById(R.id.st_bestScr);
            time = findViewById(R.id.st_Time);
            startBtn = findViewById(R.id.Startbutton);
            backBtn = findViewById(R.id.st_backB);

           backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    test_startActivity.this.finish();
                }
            });

            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check if questions are loaded before starting
                    if (DbQuery.g_questionList == null || DbQuery.g_questionList.isEmpty()) {
                        Toast.makeText(test_startActivity.this, 
                            "Questions not loaded. Please wait or try again.", 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Log question count for debugging
                    Log.d("test_startActivity", "Starting QuestionsActivity with " + 
                        DbQuery.g_questionList.size() + " questions");
                    
                    Intent intent = new Intent(test_startActivity.this, QuestionsActivity.class);
                    startActivity(intent);
                }
            });
        }



private  void setData(){

        catName.setText(DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getName());
        testNo.setText("Test No. " + (DbQuery.g_selected_test_index + 1));
        totalQues.setText(String.valueOf(DbQuery.g_questionList.size()));
        String topScore = String.valueOf(DbQuery.g_testList.get(DbQuery.g_selected_test_index).getTopScore());
        if (!topScore.endsWith("%")) topScore = topScore + "%";
        bestScore.setText(topScore);
        int minutes = DbQuery.g_testList.get(DbQuery.g_selected_test_index).getTime();
        time.setText(minutes + " min");



}
}