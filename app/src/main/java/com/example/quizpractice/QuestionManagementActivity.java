package com.example.quizpractice;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionManagementActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private RecyclerView questionsRecyclerView;
    private QuestionAdminAdapter questionAdapter;
    private List<QuestionAdminModel> questionList;
    private ProgressBar progressBar;
    private MaterialButton addQuestionBtn;
    private TextView testInfoText;
    
    private String testId;
    private String testName;
    private String categoryId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_management);
        
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        
        // Get test information from intent
        testId = getIntent().getStringExtra("TEST_ID");
        testName = getIntent().getStringExtra("TEST_NAME");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        
        if (testId == null || testName == null || categoryId == null) {
            Toast.makeText(this, "Error: Test information not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Question Management");
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Initialize UI components
        testInfoText = findViewById(R.id.testInfoText);
        testInfoText.setText("Test: " + testName);
        
        progressBar = findViewById(R.id.progressBar);
        addQuestionBtn = findViewById(R.id.addQuestionBtn);
        
        // Set up RecyclerView
        questionsRecyclerView = findViewById(R.id.questionsRecyclerView);
        questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        questionList = new ArrayList<>();
        questionAdapter = new QuestionAdminAdapter(questionList);
        questionsRecyclerView.setAdapter(questionAdapter);
        
        // Set click listeners
        questionAdapter.setOnEditClickListener(this::showEditQuestionDialog);
        questionAdapter.setOnDeleteClickListener(this::showDeleteQuestionDialog);
        
        addQuestionBtn.setOnClickListener(v -> showAddQuestionDialog());
        
        // Load questions
        loadQuestions();
    }
    
    private void loadQuestions() {
        progressBar.setVisibility(View.VISIBLE);
        questionList.clear();
        
        firestore.collection("QUIZ").document("CATEGORIES")
                .collection("CAT" + categoryId).document("TESTS")
                .collection("TEST" + testId).document("QUESTIONS_LIST")
                .collection("QUESTIONS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        String question = document.getString("QUESTION");
                        String optionA = document.getString("OPTION_A");
                        String optionB = document.getString("OPTION_B");
                        String optionC = document.getString("OPTION_C");
                        String optionD = document.getString("OPTION_D");
                        String correctAnswer = document.getString("CORRECT_ANS");
                        
                        QuestionAdminModel questionModel = new QuestionAdminModel(
                                docId, question, optionA, optionB, optionC, optionD, 
                                correctAnswer, testId, categoryId);
                        
                        questionList.add(questionModel);
                    }
                    
                    questionAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    
                    // Update test question count
                    updateQuestionCount();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Error loading questions: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
    
    private void updateQuestionCount() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("NO_OF_QUESTIONS", questionList.size());
        
        firestore.collection("QUIZ").document("CATEGORIES")
                .collection("CAT" + categoryId).document("TESTS")
                .collection("TESTS_LIST").document(testId)
                .update(testData)
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Error updating question count: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showAddQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_question, null);
        builder.setView(view);
        
        TextInputEditText questionInput = view.findViewById(R.id.questionInput);
        TextInputEditText optionAInput = view.findViewById(R.id.optionAInput);
        TextInputEditText optionBInput = view.findViewById(R.id.optionBInput);
        TextInputEditText optionCInput = view.findViewById(R.id.optionCInput);
        TextInputEditText optionDInput = view.findViewById(R.id.optionDInput);
        RadioGroup correctAnswerGroup = view.findViewById(R.id.correctAnswerGroup);
        
        builder.setTitle("Add New Question");
        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Override the positive button to prevent automatic dismissal
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString().trim();
            String optionA = optionAInput.getText().toString().trim();
            String optionB = optionBInput.getText().toString().trim();
            String optionC = optionCInput.getText().toString().trim();
            String optionD = optionDInput.getText().toString().trim();
            
            int selectedRadioButtonId = correctAnswerGroup.getCheckedRadioButtonId();
            
            if (question.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || 
                    optionC.isEmpty() || optionD.isEmpty() || selectedRadioButtonId == -1) {
                Toast.makeText(QuestionManagementActivity.this, 
                        "Please fill all fields and select a correct answer", 
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            String correctAnswer = "";
            if (selectedRadioButtonId == R.id.optionARadio) {
                correctAnswer = "A";
            } else if (selectedRadioButtonId == R.id.optionBRadio) {
                correctAnswer = "B";
            } else if (selectedRadioButtonId == R.id.optionCRadio) {
                correctAnswer = "C";
            } else if (selectedRadioButtonId == R.id.optionDRadio) {
                correctAnswer = "D";
            }
            
            addQuestion(question, optionA, optionB, optionC, optionD, correctAnswer);
            dialog.dismiss();
        });
    }
    
    private void addQuestion(String question, String optionA, String optionB, 
                           String optionC, String optionD, String correctAnswer) {
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("QUESTION", question);
        questionData.put("OPTION_A", optionA);
        questionData.put("OPTION_B", optionB);
        questionData.put("OPTION_C", optionC);
        questionData.put("OPTION_D", optionD);
        questionData.put("CORRECT_ANS", correctAnswer);
        
        firestore.collection("QUIZ").document("CATEGORIES")
                .collection("CAT" + categoryId).document("TESTS")
                .collection("TEST" + testId).document("QUESTIONS_LIST")
                .collection("QUESTIONS")
                .add(questionData)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    
                    QuestionAdminModel newQuestion = new QuestionAdminModel(
                            docId, question, optionA, optionB, optionC, optionD, 
                            correctAnswer, testId, categoryId);
                    
                    questionList.add(newQuestion);
                    questionAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Question added successfully", Toast.LENGTH_SHORT).show();
                    
                    // Update question count
                    updateQuestionCount();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Error adding question: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
    
    private void showEditQuestionDialog(QuestionAdminModel question) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_question, null);
        builder.setView(view);
        
        TextInputEditText questionInput = view.findViewById(R.id.questionInput);
        TextInputEditText optionAInput = view.findViewById(R.id.optionAInput);
        TextInputEditText optionBInput = view.findViewById(R.id.optionBInput);
        TextInputEditText optionCInput = view.findViewById(R.id.optionCInput);
        TextInputEditText optionDInput = view.findViewById(R.id.optionDInput);
        RadioGroup correctAnswerGroup = view.findViewById(R.id.correctAnswerGroup);
        
        // Fill the dialog with existing question data
        questionInput.setText(question.getQuestion());
        optionAInput.setText(question.getOptionA());
        optionBInput.setText(question.getOptionB());
        optionCInput.setText(question.getOptionC());
        optionDInput.setText(question.getOptionD());
        
        // Set the correct answer radio button
        switch (question.getCorrectAnswer()) {
            case "A":
                correctAnswerGroup.check(R.id.optionARadio);
                break;
            case "B":
                correctAnswerGroup.check(R.id.optionBRadio);
                break;
            case "C":
                correctAnswerGroup.check(R.id.optionCRadio);
                break;
            case "D":
                correctAnswerGroup.check(R.id.optionDRadio);
                break;
        }
        
        builder.setTitle("Edit Question");
        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Override the positive button to prevent automatic dismissal
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String updatedQuestion = questionInput.getText().toString().trim();
            String updatedOptionA = optionAInput.getText().toString().trim();
            String updatedOptionB = optionBInput.getText().toString().trim();
            String updatedOptionC = optionCInput.getText().toString().trim();
            String updatedOptionD = optionDInput.getText().toString().trim();
            
            int selectedRadioButtonId = correctAnswerGroup.getCheckedRadioButtonId();
            
            if (updatedQuestion.isEmpty() || updatedOptionA.isEmpty() || updatedOptionB.isEmpty() || 
                    updatedOptionC.isEmpty() || updatedOptionD.isEmpty() || selectedRadioButtonId == -1) {
                Toast.makeText(QuestionManagementActivity.this, 
                        "Please fill all fields and select a correct answer", 
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            String updatedCorrectAnswer = "";
            if (selectedRadioButtonId == R.id.optionARadio) {
                updatedCorrectAnswer = "A";
            } else if (selectedRadioButtonId == R.id.optionBRadio) {
                updatedCorrectAnswer = "B";
            } else if (selectedRadioButtonId == R.id.optionCRadio) {
                updatedCorrectAnswer = "C";
            } else if (selectedRadioButtonId == R.id.optionDRadio) {
                updatedCorrectAnswer = "D";
            }
            
            updateQuestion(question.getDocID(), updatedQuestion, updatedOptionA, updatedOptionB, 
                    updatedOptionC, updatedOptionD, updatedCorrectAnswer);
            dialog.dismiss();
        });
    }
    
    private void updateQuestion(String docId, String question, String optionA, String optionB, 
                              String optionC, String optionD, String correctAnswer) {
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("QUESTION", question);
        questionData.put("OPTION_A", optionA);
        questionData.put("OPTION_B", optionB);
        questionData.put("OPTION_C", optionC);
        questionData.put("OPTION_D", optionD);
        questionData.put("CORRECT_ANS", correctAnswer);
        
        firestore.collection("QUIZ").document("CATEGORIES")
                .collection("CAT" + categoryId).document("TESTS")
                .collection("TEST" + testId).document("QUESTIONS_LIST")
                .collection("QUESTIONS").document(docId)
                .update(questionData)
                .addOnSuccessListener(aVoid -> {
                    // Update the question in the list
                    for (int i = 0; i < questionList.size(); i++) {
                        if (questionList.get(i).getDocID().equals(docId)) {
                            questionList.get(i).setQuestion(question);
                            questionList.get(i).setOptionA(optionA);
                            questionList.get(i).setOptionB(optionB);
                            questionList.get(i).setOptionC(optionC);
                            questionList.get(i).setOptionD(optionD);
                            questionList.get(i).setCorrectAnswer(correctAnswer);
                            questionAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Question updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Error updating question: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
    
    private void showDeleteQuestionDialog(QuestionAdminModel question) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Question")
                .setMessage("Are you sure you want to delete this question?")
                .setPositiveButton("Delete", (dialog, which) -> deleteQuestion(question.getDocID()))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteQuestion(String docId) {
        progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("QUIZ").document("CATEGORIES")
                .collection("CAT" + categoryId).document("TESTS")
                .collection("TEST" + testId).document("QUESTIONS_LIST")
                .collection("QUESTIONS").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove the question from the list
                    for (int i = 0; i < questionList.size(); i++) {
                        if (questionList.get(i).getDocID().equals(docId)) {
                            questionList.remove(i);
                            questionAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Question deleted successfully", Toast.LENGTH_SHORT).show();
                    
                    // Update question count
                    updateQuestionCount();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionManagementActivity.this, 
                            "Error deleting question: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}