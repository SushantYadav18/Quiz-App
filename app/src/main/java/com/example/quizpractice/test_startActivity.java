package com.example.quizpractice;

import static com.example.quizpractice.DbQuery.loadquestions;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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
    private androidx.recyclerview.widget.RecyclerView questionGrid;
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
        loadquestions(new MyCompleteListener(){

            @Override
            public void onSuccess() {
                setData();
                setupQuestionGrid();
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

        private void init() {
            catName = findViewById(R.id.st_cty);
            testNo = findViewById(R.id.st_testNo);
            totalQues = findViewById(R.id.st_totalQues);
            bestScore = findViewById(R.id.st_bestScr);
            time = findViewById(R.id.st_Time);
            startBtn = findViewById(R.id.Startbutton);
            questionGrid = findViewById(R.id.st_question_grid);
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
                    Intent intent = new Intent(test_startActivity.this, QuestionsActivity.class);
                    startActivity(intent);
                }
            });
        }

        private void setupQuestionGrid() {
            // Display a simple grid of question numbers based on g_questionList size
            int count = DbQuery.g_questionList.size();
            if (questionGrid == null || count == 0) return;

            androidx.recyclerview.widget.GridLayoutManager layout = new androidx.recyclerview.widget.GridLayoutManager(this, 5);
            questionGrid.setLayoutManager(layout);
            questionGrid.setHasFixedSize(true);

            questionGrid.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                @Override
                public int getItemCount() {
                    return count;
                }

                @Override
                public int getItemViewType(int position) {
                    return 0;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                    android.widget.TextView tv = new android.widget.TextView(parent.getContext());
                    int pad = (int) (parent.getResources().getDisplayMetrics().density * 8);
                    tv.setPadding(pad, pad, pad, pad);
                    tv.setTextColor(android.graphics.Color.BLACK);
                    tv.setBackgroundResource(R.drawable.round_cornor);
                    tv.setTextSize(14);
                    tv.setGravity(android.view.Gravity.CENTER);
                    android.view.ViewGroup.LayoutParams lp = new android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) (parent.getResources().getDisplayMetrics().density * 40)
                    );
                    tv.setLayoutParams(lp);
                    return new androidx.recyclerview.widget.RecyclerView.ViewHolder(tv) {};
                }

                @Override
                public void onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                    android.widget.TextView tv = (android.widget.TextView) holder.itemView;
                    tv.setText(String.valueOf(position + 1));
                }
            });
        }

private  void setData(){

        catName.setText(DbQuery.g_catList.get(DbQuery.g_selected_cat_index).getName());
        testNo.setText("Test No." + String.valueOf(DbQuery.g_selected_test_index + 1));
        totalQues.setText(String.valueOf(DbQuery.g_questionList.size()));
        bestScore.setText(String.valueOf(DbQuery.g_testList.get(DbQuery.g_selected_test_index).getTopScore()));
        time.setText(String.valueOf(DbQuery.g_testList.get(DbQuery.g_selected_test_index).getTime()));



}
}