package com.example.quizpractice;

import android.util.Log;
import android.util.Printer;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Random;

public class DbQuery {

    public static FirebaseFirestore g_firestore = FirebaseFirestore.getInstance();
    public static List<CategoryModel> g_catList = new ArrayList<>();
    public static List<TestModel> g_testList = new ArrayList<>();
    public static List<QuestionModel> g_questionList = new ArrayList<>();
    public static List<RankModel> g_rankList = new ArrayList<>();
    
    // Admin-specific lists
    public static List<CategoryModel> g_adminCatList = new ArrayList<>();
    public static List<TestAdminModel> g_adminTestList = new ArrayList<>();
    public static List<QuestionAdminModel> g_adminQuestionList = new ArrayList<>();

    public static int g_selected_cat_index = 0;
    public static int g_selected_test_index = 0;
    public static int g_quesIndex = 0;

    public static ProfileModel myProfile = new ProfileModel();
    public static int g_total_question = 0;

    public static void createUserData( String name, String email, MyCompleteListener completeListener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("NAME", name);
        userData.put("EMAIL_ID", email);
        userData.put("TOTAL_SCORE", 0);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        g_firestore.collection("USERS").document(userId).set(userData)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    // Set profile data
                    myProfile.setUserId(userId);
                    myProfile.setName(name);
                    myProfile.setEmail(email);
                    myProfile.setProfile("null");
                    
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    completeListener.onFailure();
                }
            });
    }

    public  static  void  getUserData(final MyCompleteListener completeListener){
        g_firestore.collection("USERS").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        myProfile.setUserId(userId);
                        myProfile.setName(documentSnapshot.getString("NAME"));
                        myProfile.setEmail(documentSnapshot.getString("EMAIL_ID"));
                        myProfile.setProfile("null");
                        
                        Long totalScore = documentSnapshot.getLong("TOTAL_SCORE");
                        if (totalScore != null) {
                            // Store total score in profile if needed
                            // myProfile.setTotalScore(totalScore.intValue()); // Uncomment if you add this field
                        }
                    }
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    completeListener.onFailure();
                }
            });
    }

    public static void loadCategories(final MyCompleteListener completeListener) {
        g_catList.clear();
        Log.d("DbQuery", "Starting to load categories from database...");

        g_firestore.collection("QUIZ").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        try {
                            Log.d("DbQuery", "Total documents found in QUIZ collection: " + queryDocumentSnapshots.size());

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Log.d("DbQuery", "Processing document: " + doc.getId());
                                Log.d("DbQuery", "Document data: " + doc.getData());

                                // Skip the Categories document if it exists
                                if (doc.getId().equals("Categories")) {
                                    Log.d("DbQuery", "Skipping Categories document");
                                    continue;
                                }

                                String catName = doc.getString("NAME");
                                Long noOfTests = null;

                                // Try to get NO_OF_TESTS as a Long first
                                try {
                                    noOfTests = doc.getLong("NO_OF_TESTS");
                                } catch (Exception e) {
                                    // If that fails, try to get it as a String and parse it
                                    try {
                                        String noOfTestsStr = doc.getString("NO_OF_TESTS");
                                        if (noOfTestsStr != null) {
                                            noOfTests = Long.parseLong(noOfTestsStr);
                                        }
                                    } catch (Exception e2) {
                                        Log.e("DbQuery", "Could not parse NO_OF_TESTS for document: " + doc.getId());
                                    }
                                }

                                if (catName != null && noOfTests != null) {
                                    Log.d("DbQuery", "Adding category: " + catName + " with " + noOfTests + " tests");
                                    g_catList.add(new CategoryModel(doc.getId(), catName, noOfTests.intValue()));
                                } else {
                                    Log.e("DbQuery", "Missing NAME or NO_OF_TESTS for document: " + doc.getId());
                                    Log.e("DbQuery", "NAME: " + catName + ", NO_OF_TESTS: " + noOfTests);
                                }
                            }

                            Log.d("DbQuery", "Total categories loaded: " + g_catList.size());
                            if (g_catList.isEmpty()) {
                                Log.w("DbQuery", "No categories found in database. This might be normal if database is empty.");
                            }
                            completeListener.onSuccess();
                        } catch (Exception e) {
                            Log.e("DbQuery", "Error loading categories: " + e.getMessage());
                            e.printStackTrace();
                            completeListener.onFailure();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Failed to load categories: " + e.getMessage());
                        e.printStackTrace();
                        completeListener.onFailure();
                    }
                });
    }

    public static void loadquestions(final MyCompleteListener completeListener) {
        g_questionList.clear();
        Log.d("DbQuery", "Starting loadquestions method");

        if (g_catList.isEmpty() || g_selected_cat_index >= g_catList.size() ||
            g_testList.isEmpty() || g_selected_test_index >= g_testList.size()) {
            Log.e("DbQuery", "Cannot load questions: invalid selected indices. catIndex=" + g_selected_cat_index +
                    ", testIndex=" + g_selected_test_index + ", cats=" + g_catList.size() + ", tests=" + g_testList.size());
            completeListener.onFailure();
            return;
        }

        final String expectedCategoryId = g_catList.get(g_selected_cat_index).getDocID();
        final String expectedTestId = g_testList.get(g_selected_test_index).getId();

        Log.d("DbQuery", "Attempting to load questions for categoryId=" + expectedCategoryId + ", testId=" + expectedTestId);
        Log.d("DbQuery", "Category name: " + g_catList.get(g_selected_cat_index).getName());
        Log.d("DbQuery", "Test ID: " + expectedTestId);
        Log.d("DbQuery", "Category list size: " + g_catList.size());
        Log.d("DbQuery", "Test list size: " + g_testList.size());

                 // Try multiple database structures - start with your actual collection name
         attemptLoadQuestions("Questions", "CATEGORY", "TEST", expectedCategoryId, expectedTestId, new MyCompleteListener() {
            @Override
            public void onSuccess() {
                if (g_questionList.isEmpty()) {
                    Log.w("DbQuery", "Primary query returned 0 questions. Trying alternative structures...");
                    
                    // Try different collection names
                    attemptLoadQuestions("QUESTIONS", "CATEGORY", "TEST", expectedCategoryId, expectedTestId, new MyCompleteListener() {
                        @Override
                        public void onSuccess() {
                            if (g_questionList.isEmpty()) {
                                // Try different field names
                                attemptLoadQuestions("Question", "CAT_ID", "TEST_ID", expectedCategoryId, expectedTestId, new MyCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        if (g_questionList.isEmpty()) {
                                            // Try with lowercase field names
                                            attemptLoadQuestions("Question", "category", "test", expectedCategoryId, expectedTestId, new MyCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    if (g_questionList.isEmpty()) {
                                                        // Try with different collection structure
                                                        attemptLoadQuestions("questions", "category_id", "test_id", expectedCategoryId, expectedTestId, new MyCompleteListener() {
                                                            @Override
                                                            public void onSuccess() {
                                                                if (g_questionList.isEmpty()) {
                                                                    Log.e("DbQuery", "No questions found after all attempts. Database structure may be different.");
                                                                    Log.e("DbQuery", "Please check your Firestore database for the correct collection and field names.");
                                                                    completeListener.onSuccess(); // Succeed with empty list
                                                                } else {
                                                                    Log.d("DbQuery", "Found " + g_questionList.size() + " questions using lowercase structure");
                                                                    completeListener.onSuccess();
                                                                }
                                                            }

                                                            @Override
                                                            public void onFailure() {
                                                                Log.e("DbQuery", "All query attempts failed. Please verify database structure.");
                                                                completeListener.onFailure();
                                                            }
                                                        });
                                                    } else {
                                                        Log.d("DbQuery", "Found " + g_questionList.size() + " questions using lowercase fields");
                                                        completeListener.onSuccess();
                                                    }
                                                }

                                                @Override
                                                public void onFailure() {
                                                    Log.e("DbQuery", "Lowercase fields query failed");
                                                    completeListener.onFailure();
                                                }
                                            });
                                        } else {
                                            Log.d("DbQuery", "Found " + g_questionList.size() + " questions using CAT_ID/TEST_ID fields");
                                            completeListener.onSuccess();
                                        }
                                    }

                                    @Override
                                    public void onFailure() {
                                        Log.e("DbQuery", "CAT_ID/TEST_ID query failed");
                                        completeListener.onFailure();
                                    }
                                });
                            } else {
                                Log.d("DbQuery", "Found " + g_questionList.size() + " questions using QUESTIONS collection");
                                completeListener.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure() {
                            Log.e("DbQuery", "QUESTIONS collection query failed");
                            completeListener.onFailure();
                        }
                    });
                } else {
                    Log.d("DbQuery", "Found " + g_questionList.size() + " questions using primary query");
                    completeListener.onSuccess();
                }
            }

            @Override
            public void onFailure() {
                Log.e("DbQuery", "Primary query failed");
                completeListener.onFailure();
            }
        });
    }

    private static void attemptLoadQuestions(String collection,
                                             String categoryField,
                                             String testField,
                                             String categoryId,
                                             String testId,
                                             final MyCompleteListener completeListener) {
        Log.d("DbQuery", "Querying collection='" + collection + "' where " + categoryField + "='" + categoryId + "' AND " + testField + "='" + testId + "'");
        Log.d("DbQuery", "Looking for questions with categoryId: " + categoryId + " and testId: " + testId);

        g_firestore.collection(collection)
                .whereEqualTo(categoryField, categoryId)
                .whereEqualTo(testField, testId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Log.d("DbQuery", "Questions query returned " + queryDocumentSnapshots.size() + " docs");
                        
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.w("DbQuery", "No questions found with the specified criteria");
                            Log.w("DbQuery", "This might mean the categoryId or testId don't match what's in the database");
                            
                            // Let's check what's actually in the collection
                            g_firestore.collection(collection).limit(5).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot allQuestions) {
                                        Log.d("DbQuery", "Sample of all questions in " + collection + " collection:");
                                        for (DocumentSnapshot doc : allQuestions) {
                                            Log.d("DbQuery", "  - Doc ID: " + doc.getId());
                                            Log.d("DbQuery", "  - Category field (" + categoryField + "): " + doc.getString(categoryField));
                                            Log.d("DbQuery", "  - Test field (" + testField + "): " + doc.getString(testField));
                                        }
                                        completeListener.onSuccess();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("DbQuery", "Failed to get sample questions: " + e.getMessage());
                                        completeListener.onSuccess();
                                    }
                                });
                            return;
                        }
                        
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                Log.d("DbQuery", "Processing question document: " + doc.getId());
                                Log.d("DbQuery", "Question data: " + doc.getData());
                                
                                int answerIndex;
                                try {
                                    Long ans = doc.getLong("ANSWER");
                                    answerIndex = (ans != null) ? ans.intValue() : 0;
                                } catch (Exception e) {
                                    String ansStr = doc.getString("ANSWER");
                                    answerIndex = ansStr != null ? Integer.parseInt(ansStr) : 0;
                                }

                                g_questionList.add(new QuestionModel(
                                        doc.getString("QUESTION"),
                                        doc.getString("A"),
                                        doc.getString("B"),
                                        doc.getString("C"),
                                        doc.getString("D"),
                                        answerIndex
                                ));
                                
                                Log.d("DbQuery", "Successfully added question: " + doc.getString("QUESTION"));
                            } catch (Exception e) {
                                Log.e("DbQuery", "Error parsing question doc: " + doc.getId() + ", err=" + e.getMessage());
                            }
                        }
                        
                        
                        // Shuffle questions using Fisher-Yates algorithm
                        if (!g_questionList.isEmpty()) {
                            shuffleQuestions();
                        }
                        
                        completeListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Questions query failed for collection=" + collection + ", err=" + e.getMessage());
                        completeListener.onFailure();
                    }
                });
    }




    public static void loadTests(final MyCompleteListener completeListener) {
        g_testList.clear();
        
        if (g_catList.isEmpty() || g_selected_cat_index >= g_catList.size()) {
            Log.e("DbQuery", "No category selected or invalid index. Selected index: " + g_selected_cat_index + ", Total categories: " + g_catList.size());
            completeListener.onFailure();
            return;
        }

        String categoryDocId = g_catList.get(g_selected_cat_index).getDocID();
        Log.d("DbQuery", "Loading tests for category: " + categoryDocId);
        Log.d("DbQuery", "Category name: " + g_catList.get(g_selected_cat_index).getName());
        Log.d("DbQuery", "Number of tests in category: " + g_catList.get(g_selected_cat_index).getNoOfTests());

        // Log the full path we're trying to access
        String fullPath = "QUIZ/" + categoryDocId + "/TESTS_LIST/TESTS_INFO";
        Log.d("DbQuery", "Attempting to access Firestore path: " + fullPath);

        g_firestore.collection("QUIZ")
                .document(categoryDocId)
                .collection("TESTS_LIST")
                .document("TESTS_INFO")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try {
                            if (!documentSnapshot.exists()) {
                                Log.e("DbQuery", "TESTS_INFO document does not exist at path: " + fullPath);
                                completeListener.onFailure();
                                return;
                            }

                            Log.d("DbQuery", "Document exists. Data: " + documentSnapshot.getData());

                            int noOfTests = g_catList.get(g_selected_cat_index).getNoOfTests();
                            Log.d("DbQuery", "Loading " + noOfTests + " tests for category: " + categoryDocId);

                            for (int i = 1; i <= noOfTests; i++) {
                            final int testIndex = i;
                            String testIdKey = "TEST" + testIndex + "_ID";
                            String testTimeKey = "TEST" + testIndex + "_TIME";
                            String testDifficultyKey = "TEST" + testIndex + "_DIFFICULTY";
                            String testRequiredScoreKey = "TEST" + testIndex + "_REQUIRED_SCORE";
                                
                                String testId = documentSnapshot.getString(testIdKey);
                                Long testTime = documentSnapshot.getLong(testTimeKey);
                                String testDifficulty = documentSnapshot.getString(testDifficultyKey);
                                Long testRequiredScore = documentSnapshot.getLong(testRequiredScoreKey);

                                Log.d("DbQuery", "Checking test " + i + ":");
                                Log.d("DbQuery", "  - " + testIdKey + " = " + testId);
                                Log.d("DbQuery", "  - " + testTimeKey + " = " + testTime);
                                Log.d("DbQuery", "  - " + testDifficultyKey + " = " + testDifficulty);
                                Log.d("DbQuery", "  - " + testRequiredScoreKey + " = " + testRequiredScore);

                                if (testId != null && testTime != null) {
                                    // Set default difficulty if not specified
                                    if (testDifficulty == null) {
                                        if (i <= 3) {
                                            testDifficulty = "EASY";
                                        } else if (i <= 6) {
                                            testDifficulty = "MEDIUM";
                                        } else {
                                            testDifficulty = "HARD";
                                        }
                                    }
                                    
                                    // Set default required score if not specified
                                    int requiredScore = 0;
                                    if (testRequiredScore != null) {
                                        requiredScore = testRequiredScore.intValue();
                                    } else {
                                        if (testDifficulty.equals("MEDIUM")) {
                                            requiredScore = 70;
                                        } else if (testDifficulty.equals("HARD")) {
                                            requiredScore = 85;
                                        }
                                    }
                                    
                                    Log.d("DbQuery", "Adding test: " + testId + " with time: " + testTime + 
                                          ", difficulty: " + testDifficulty + ", required score: " + requiredScore);
                                    
                                    g_testList.add(new TestModel(
                                        testId,
                                        0, // Initial score
                                        testTime.intValue(),
                                        testDifficulty,
                                        requiredScore
                                    ));
                                } else {
                                    Log.e("DbQuery", "Missing data for test " + i + 
                                        " (ID: " + (testId == null ? "null" : testId) + 
                                        ", Time: " + (testTime == null ? "null" : testTime) + ")");
                                }
                            }

                            Log.d("DbQuery", "Successfully loaded " + g_testList.size() + " tests");
                            if (g_testList.isEmpty()) {
                                Log.e("DbQuery", "No tests were loaded despite document existing");
                                completeListener.onFailure();
                            } else {
                                completeListener.onSuccess();
                            }
                        } catch (Exception e) {
                            Log.e("DbQuery", "Error loading tests: " + e.getMessage());
                            e.printStackTrace();
                            completeListener.onFailure();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Failed to load tests: " + e.getMessage());
                        Log.e("DbQuery", "Error type: " + e.getClass().getName());
                        e.printStackTrace();
                        completeListener.onFailure();
                    }
                });
    }

    public  static void  loadData(final MyCompleteListener completeListener){

        DbQuery.loadCategories(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                getUserData(completeListener);

            }
            @Override
            public void onFailure() {
                completeListener.onFailure();
            }
        });
    }

    public static void debugDatabaseStructure(final MyCompleteListener completeListener) {
        Log.d("DbQuery", "=== DEBUGGING DATABASE STRUCTURE ===");
        
        // List all collections
        g_firestore.collection("QUIZ").get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    Log.d("DbQuery", "QUIZ collection has " + queryDocumentSnapshots.size() + " documents:");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d("DbQuery", "  - Document ID: " + doc.getId());
                        Log.d("DbQuery", "  - Document data: " + doc.getData());
                    }
                    
                                         // Try to find questions collection - check your actual collection name
                     g_firestore.collection("Questions").get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot questionSnapshot) {
                                Log.d("DbQuery", "Question collection has " + questionSnapshot.size() + " documents:");
                                if (!questionSnapshot.isEmpty()) {
                                    DocumentSnapshot firstDoc = questionSnapshot.getDocuments().iterator().next();
                                    Log.d("DbQuery", "  - Sample question document: " + firstDoc.getData());
                                    Log.d("DbQuery", "  - Sample question fields: " + firstDoc.getData().keySet());
                                }
                                
                                // Try QUESTIONS collection too
                                g_firestore.collection("QUESTIONS").get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot questionsSnapshot) {
                                            Log.d("DbQuery", "QUESTIONS collection has " + questionsSnapshot.size() + " documents:");
                                            if (!questionsSnapshot.isEmpty()) {
                                                DocumentSnapshot firstDoc = questionsSnapshot.getDocuments().iterator().next();
                                                Log.d("DbQuery", "  - Sample questions document: " + firstDoc.getData());
                                                Log.d("DbQuery", "  - Sample questions fields: " + firstDoc.getData().keySet());
                                            }
                                            completeListener.onSuccess();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("DbQuery", "QUESTIONS collection doesn't exist or access denied");
                                            completeListener.onSuccess();
                                        }
                                    });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("DbQuery", "Question collection doesn't exist or access denied");
                                completeListener.onSuccess();
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to access QUIZ collection: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }

    // Save user quiz result to database
    public static void saveResult(int score, final MyCompleteListener completeListener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("DbQuery", "User not authenticated");
            completeListener.onFailure();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("DbQuery", "Saving result for user: " + userId + " with score: " + score);

        // Create a batch write for atomic operations
        WriteBatch batch = g_firestore.batch();

        // 1. Update user's total score
        DocumentReference userDoc = g_firestore.collection("USERS").document(userId);
        batch.update(userDoc, "TOTAL_SCORE", FieldValue.increment(score));

        // 2. Save individual test result (this will overwrite previous score for re-attempts)
        if (g_selected_cat_index >= 0 && g_selected_cat_index < g_catList.size() &&
            g_selected_test_index >= 0 && g_selected_test_index < g_testList.size()) {
            
            String categoryId = g_catList.get(g_selected_cat_index).getDocID();
            String testId = g_testList.get(g_selected_test_index).getId();
            
            // Create or update test result document
            DocumentReference testResultDoc = userDoc.collection("TEST_RESULTS")
                .document(categoryId + "_" + testId);
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("CATEGORY_ID", categoryId);
            testResult.put("TEST_ID", testId);
            testResult.put("SCORE", score);
            testResult.put("MAX_SCORE", g_questionList.size());
            testResult.put("TIMESTAMP", FieldValue.serverTimestamp());
            testResult.put("ATTEMPT_COUNT", FieldValue.increment(1));
            
            batch.set(testResultDoc, testResult, SetOptions.merge());
            
            Log.d("DbQuery", "Saving test result for category: " + categoryId + ", test: " + testId);
        }

        // 3. Update leaderboard if this is a new high score
        DocumentReference leaderboardDoc = g_firestore.collection("LEADERBOARD")
            .document("GLOBAL");
        
        Map<String, Object> leaderboardData = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("NAME", myProfile.getName());
        userData.put("EMAIL", myProfile.getEmail());
        userData.put("TOTAL_SCORE", FieldValue.increment(score));
        userData.put("LAST_UPDATED", FieldValue.serverTimestamp());
        leaderboardData.put(userId, userData);
        
        batch.set(leaderboardDoc, leaderboardData, SetOptions.merge());

        // Execute the batch
        batch.commit()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DbQuery", "Result saved successfully");
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to save result: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }

    // Get user's test result for a specific test
    public static void getUserTestResult(String categoryId, String testId, final MyCompleteListener completeListener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            completeListener.onFailure();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference testResultDoc = g_firestore.collection("USERS")
            .document(userId)
            .collection("TEST_RESULTS")
            .document(categoryId + "_" + testId);

        testResultDoc.get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        Log.d("DbQuery", "Found existing test result: " + documentSnapshot.getData());
                        // You can return the data here if needed
                    }
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to get test result: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }

    // Shuffle questions using Fisher-Yates algorithm
    public static void shuffleQuestions() {
        if (g_questionList == null || g_questionList.isEmpty()) {
            Log.d("DbQuery", "No questions to shuffle");
            return;
        }
        
        Random random = new Random();
        for (int i = g_questionList.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            // Swap elements at i and j
            QuestionModel temp = g_questionList.get(i);
            g_questionList.set(i, g_questionList.get(j));
            g_questionList.set(j, temp);
        }
        
        Log.d("DbQuery", "Questions reshuffled using Fisher-Yates. Total questions: " + g_questionList.size());
    }
    
    // Clear previous test result for re-attempt
    public static void clearTestResult(String categoryId, String testId, final MyCompleteListener completeListener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            completeListener.onFailure();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference testResultDoc = g_firestore.collection("USERS")
            .document(userId)
            .collection("TEST_RESULTS")
            .document(categoryId + "_" + testId);

        testResultDoc.delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DbQuery", "Previous test result cleared for re-attempt");
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to clear test result: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }

    // Load leaderboard data from database
    public static void loadLeaderboardData(final MyCompleteListener completeListener) {
        g_rankList.clear();
        
        // First, get all users with their scores
        g_firestore.collection("USERS").get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();
                        
                        // Skip the TOTAL_USERS document
                        if ("TOTAL_USERS".equals(userId)) {
                            continue;
                        }
                        
                        String name = document.getString("NAME");
                        String email = document.getString("EMAIL_ID");
                        Long totalScore = document.getLong("TOTAL_SCORE");
                        
                        if (name != null && email != null && totalScore != null) {
                            RankModel rankModel = new RankModel(
                                userId,
                                name,
                                email,
                                totalScore.intValue(),
                                0, // Rank will be calculated later
                                System.currentTimeMillis()
                            );
                            g_rankList.add(rankModel);
                        }
                    }
                    
                    // Sort by score (highest first) and assign ranks
                    Collections.sort(g_rankList, (r1, r2) -> 
                        Integer.compare(r2.getTotalScore(), r1.getTotalScore()));
                    
                    for (int i = 0; i < g_rankList.size(); i++) {
                        g_rankList.get(i).setRank(i + 1);
                    }
                    
                    Log.d("DbQuery", "Loaded " + g_rankList.size() + " users for leaderboard");
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to load leaderboard data: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }

    // Get user's current rank
    public static int getUserRank(String userId) {
        for (RankModel rank : g_rankList) {
            if (rank.getUserId().equals(userId)) {
                return rank.getRank();
            }
        }
        return -1; // User not found
    }

    // Get user's current score
    public static int getUserScore(String userId) {
        for (RankModel rank : g_rankList) {
            if (rank.getUserId().equals(userId)) {
                return rank.getTotalScore();
            }
        }
        return 0; // User not found
    }
    
    // ==================== ADMIN OPERATIONS ====================
    
    // Load categories for admin
    public static void loadCategoriesForAdmin(final MyCompleteListener completeListener) {
        g_adminCatList.clear();
        Log.d("DbQuery", "Loading categories for admin...");

        g_firestore.collection("QUIZ").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        try {
                            Log.d("DbQuery", "Total documents found in QUIZ collection: " + queryDocumentSnapshots.size());

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                // Skip the Categories document if it exists
                                if (doc.getId().equals("Categories")) {
                                    continue;
                                }

                                String catName = doc.getString("NAME");
                                Long noOfTests = null;

                                // Try to get NO_OF_TESTS as a Long first
                                try {
                                    noOfTests = doc.getLong("NO_OF_TESTS");
                                } catch (Exception e) {
                                    // If that fails, try to get it as a String and parse it
                                    try {
                                        String noOfTestsStr = doc.getString("NO_OF_TESTS");
                                        if (noOfTestsStr != null) {
                                            noOfTests = Long.parseLong(noOfTestsStr);
                                        }
                                    } catch (Exception e2) {
                                        Log.e("DbQuery", "Could not parse NO_OF_TESTS for document: " + doc.getId());
                                    }
                                }

                                if (catName != null && noOfTests != null) {
                                    g_adminCatList.add(new CategoryModel(doc.getId(), catName, noOfTests.intValue()));
                                }
                            }

                            completeListener.onSuccess();
                        } catch (Exception e) {
                            Log.e("DbQuery", "Error loading categories for admin: " + e.getMessage());
                            e.printStackTrace();
                            completeListener.onFailure();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Failed to load categories for admin: " + e.getMessage());
                        completeListener.onFailure();
                    }
                });
    }
    
    // Add a new category
    public static void addCategory(String categoryName, final MyCompleteListener completeListener) {
        // Create a new document with auto-generated ID
        DocumentReference newCategoryRef = g_firestore.collection("QUIZ").document();
        
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("NAME", categoryName);
        categoryData.put("NO_OF_TESTS", 0);
        
        newCategoryRef.set(categoryData)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Create TESTS_INFO document in TESTS_LIST subcollection
                    DocumentReference testsInfoRef = newCategoryRef.collection("TESTS_LIST").document("TESTS_INFO");
                    testsInfoRef.set(new HashMap<>())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("DbQuery", "Category added successfully: " + categoryName);
                                completeListener.onSuccess();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("DbQuery", "Failed to create TESTS_INFO document: " + e.getMessage());
                                completeListener.onFailure();
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to add category: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Update a category
    public static void updateCategory(String categoryId, String newCategoryName, final MyCompleteListener completeListener) {
        DocumentReference categoryRef = g_firestore.collection("QUIZ").document(categoryId);
        
        categoryRef.update("NAME", newCategoryName)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DbQuery", "Category updated successfully: " + categoryId);
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to update category: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Delete a category
    public static void deleteCategory(String categoryId, final MyCompleteListener completeListener) {
        // First, get the TESTS_INFO document to find all tests in this category
        DocumentReference testsInfoRef = g_firestore.collection("QUIZ").document(categoryId)
                .collection("TESTS_LIST").document("TESTS_INFO");
        
        testsInfoRef.get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        // Get all test IDs from this category
                        Map<String, Object> data = documentSnapshot.getData();
                        List<String> testIds = new ArrayList<>();
                        
                        if (data != null) {
                            for (String key : data.keySet()) {
                                if (key.endsWith("_ID")) {
                                    String testId = (String) data.get(key);
                                    if (testId != null) {
                                        testIds.add(testId);
                                    }
                                }
                            }
                        }
                        
                        // Delete all questions for each test in this category
                        deleteQuestionsForTests(categoryId, testIds, new MyCompleteListener() {
                            @Override
                            public void onSuccess() {
                                // Now delete the category document
                                g_firestore.collection("QUIZ").document(categoryId).delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("DbQuery", "Category deleted successfully: " + categoryId);
                                            completeListener.onSuccess();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("DbQuery", "Failed to delete category: " + e.getMessage());
                                            completeListener.onFailure();
                                        }
                                    });
                            }
                            
                            @Override
                            public void onFailure() {
                                Log.e("DbQuery", "Failed to delete questions for tests in category: " + categoryId);
                                completeListener.onFailure();
                            }
                        });
                    } else {
                        // If TESTS_INFO doesn't exist, just delete the category
                        g_firestore.collection("QUIZ").document(categoryId).delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("DbQuery", "Category deleted successfully: " + categoryId);
                                    completeListener.onSuccess();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("DbQuery", "Failed to delete category: " + e.getMessage());
                                    completeListener.onFailure();
                                }
                            });
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to get TESTS_INFO for category: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Helper method to delete questions for multiple tests
    private static void deleteQuestionsForTests(String categoryId, List<String> testIds, final MyCompleteListener completeListener) {
        if (testIds == null || testIds.isEmpty()) {
            completeListener.onSuccess();
            return;
        }
        
        // Use a batch to delete questions for all tests
        final WriteBatch batch = g_firestore.batch();
        final int[] completedQueries = {0};
        final boolean[] anyFailure = {false};
        
        // Helper function to check if all queries are complete
        final Runnable checkAllQueriesComplete = new Runnable() {
            @Override
            public void run() {
                if (completedQueries[0] >= testIds.size()) {
                    if (anyFailure[0]) {
                        completeListener.onFailure();
                    } else {
                        // Commit the batch delete
                        batch.commit()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("DbQuery", "Successfully deleted questions for tests");
                                    completeListener.onSuccess();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("DbQuery", "Failed to delete questions in batch: " + e.getMessage());
                                    completeListener.onFailure();
                                }
                            });
                    }
                }
            }
        };
        
        for (String testId : testIds) {
            // Try different collection names for questions
            g_firestore.collection("Questions")
                .whereEqualTo("CATEGORY", categoryId)
                .whereEqualTo("TEST", testId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            batch.delete(doc.getReference());
                        }
                        
                        completedQueries[0]++;
                        checkAllQueriesComplete.run();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Failed to query questions: " + e.getMessage());
                        anyFailure[0] = true;
                        completedQueries[0]++;
                        checkAllQueriesComplete.run();
                    }
                });
        }
    }
    
    // ==================== TEST ADMIN OPERATIONS ====================
    
    // Load tests for admin
    public static void loadTestsForAdmin(String categoryId, final MyCompleteListener completeListener) {
        g_adminTestList.clear();
        
        // Get the TESTS_INFO document
        g_firestore.collection("QUIZ").document(categoryId)
                .collection("TESTS_LIST").document("TESTS_INFO")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (!documentSnapshot.exists()) {
                            completeListener.onSuccess(); // No tests yet
                            return;
                        }
                        
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data == null) {
                            completeListener.onSuccess(); // Empty document
                            return;
                        }
                        
                        // Parse test data
                        int noOfTestsTemp = 0;
                        try {
                            Long noOfTestsLong = documentSnapshot.getLong("NO_OF_TESTS");
                            if (noOfTestsLong != null) {
                                noOfTestsTemp = noOfTestsLong.intValue();
                            }
                        } catch (Exception e) {
                            // If NO_OF_TESTS field doesn't exist, count the test entries
                            for (String key : data.keySet()) {
                                if (key.endsWith("_ID")) {
                                    noOfTestsTemp++;
                                }
                            }
                        }
                        final int noOfTests = noOfTestsTemp;
                        
                        for (int i = 1; i <= noOfTests; i++) {
                            final int testIndex = i;
                            String testIdKey = "TEST" + testIndex + "_ID";
                            String testTimeKey = "TEST" + testIndex + "_TIME";
                            
                            String testId = documentSnapshot.getString(testIdKey);
                            Long testTime = documentSnapshot.getLong(testTimeKey);
                            
                            if (testId != null && testTime != null) {
                                // Get question count for this test
                                getQuestionCountForTest(categoryId, testId, new MyCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        // Question count is stored in g_total_question
                                        g_adminTestList.add(new TestAdminModel(
                                            testId,
                                            "Test " + testIndex, // Default name, will be updated
                                            testTime.intValue(),
                                            g_total_question,
                                            categoryId
                                        ));
                                        
                                        // If this is the last test, notify completion
                                        if (g_adminTestList.size() == noOfTests) {
                                            completeListener.onSuccess();
                                        }
                                    }
                                    
                                    @Override
                                    public void onFailure() {
                                        // Add test with 0 questions
                                        g_adminTestList.add(new TestAdminModel(
                                            testId,
                                            "Test " + testIndex, // Default name, will be updated
                                            testTime.intValue(),
                                            0,
                                            categoryId
                                        ));
                                        
                                        // If this is the last test, notify completion
                                        if (g_adminTestList.size() == noOfTests) {
                                            completeListener.onSuccess();
                                        }
                                    }
                                });
                            }
                        }
                        
                        // If no tests were found, notify completion
                        if (noOfTests == 0) {
                            completeListener.onSuccess();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DbQuery", "Failed to load tests for admin: " + e.getMessage());
                        completeListener.onFailure();
                    }
                });
    }
    
    // Get question count for a test
    private static void getQuestionCountForTest(String categoryId, String testId, final MyCompleteListener completeListener) {
        g_total_question = 0;
        
        // Try different collection names for questions
        g_firestore.collection("Questions")
            .whereEqualTo("CATEGORY", categoryId)
            .whereEqualTo("TEST", testId)
            .get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    g_total_question = queryDocumentSnapshots.size();
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to get question count: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Add a new test
    public static void addTest(String categoryId, String testName, int timeInMinutes, final MyCompleteListener completeListener) {
        // First, get the current TESTS_INFO document
        DocumentReference testsInfoRef = g_firestore.collection("QUIZ").document(categoryId)
                .collection("TESTS_LIST").document("TESTS_INFO");
        
        testsInfoRef.get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    int noOfTests = 0;
                    
                    if (documentSnapshot.exists()) {
                        Long noOfTestsLong = documentSnapshot.getLong("NO_OF_TESTS");
                        if (noOfTestsLong != null) {
                            noOfTests = noOfTestsLong.intValue();
                        } else {
                            // Count test entries if NO_OF_TESTS field doesn't exist
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                for (String key : data.keySet()) {
                                    if (key.endsWith("_ID")) {
                                        noOfTests++;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Increment test count
                    final int updatedNoOfTests = noOfTests + 1;
                    
                    // Create a new test ID
                    String testId = categoryId + "_TEST" + updatedNoOfTests;
                    
                    // Update TESTS_INFO document
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("NO_OF_TESTS", updatedNoOfTests);
                    updates.put("TEST" + updatedNoOfTests + "_ID", testId);
                    updates.put("TEST" + updatedNoOfTests + "_TIME", timeInMinutes);
                    updates.put("TEST" + updatedNoOfTests + "_NAME", testName);
                    
                    testsInfoRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Update category document with new test count
                                g_firestore.collection("QUIZ").document(categoryId)
                                    .update("NO_OF_TESTS", updatedNoOfTests)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("DbQuery", "Test added successfully: " + testId);
                                            completeListener.onSuccess();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("DbQuery", "Failed to update category test count: " + e.getMessage());
                                            completeListener.onFailure();
                                        }
                                    });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("DbQuery", "Failed to add test: " + e.getMessage());
                                completeListener.onFailure();
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to get TESTS_INFO: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Update an existing test
    public static void updateTest(String categoryId, String testId, String testName, int timeInMinutes, final MyCompleteListener completeListener) {
        // Find the test in TESTS_INFO document
        DocumentReference testsInfoRef = g_firestore.collection("QUIZ").document(categoryId)
                .collection("TESTS_LIST").document("TESTS_INFO");
        
        testsInfoRef.get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (!documentSnapshot.exists()) {
                        Log.e("DbQuery", "TESTS_INFO document does not exist");
                        completeListener.onFailure();
                        return;
                    }
                    
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data == null) {
                        Log.e("DbQuery", "TESTS_INFO document is empty");
                        completeListener.onFailure();
                        return;
                    }
                    
                    // Find which test number corresponds to this testId
                    int testNumber = -1;
                    for (String key : data.keySet()) {
                        if (key.endsWith("_ID") && testId.equals(data.get(key))) {
                            try {
                                testNumber = Integer.parseInt(key.substring(4, key.length() - 3));
                                break;
                            } catch (NumberFormatException e) {
                                Log.e("DbQuery", "Failed to parse test number from key: " + key);
                            }
                        }
                    }
                    
                    if (testNumber == -1) {
                        Log.e("DbQuery", "Test ID not found in TESTS_INFO: " + testId);
                        completeListener.onFailure();
                        return;
                    }
                    
                    // Update test information
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("TEST" + testNumber + "_TIME", timeInMinutes);
                    updates.put("TEST" + testNumber + "_NAME", testName);
                    
                    testsInfoRef.update(updates)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("DbQuery", "Test updated successfully: " + testId);
                                completeListener.onSuccess();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("DbQuery", "Failed to update test: " + e.getMessage());
                                completeListener.onFailure();
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to get TESTS_INFO: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Delete a test
    public static void deleteTest(String categoryId, String testId, final MyCompleteListener completeListener) {
        // First delete all questions for this test
        List<String> testIds = new ArrayList<>();
        testIds.add(testId);
        
        deleteQuestionsForTests(categoryId, testIds, new MyCompleteListener() {
            @Override
            public void onSuccess() {
                // Now find and remove the test from TESTS_INFO
                DocumentReference testsInfoRef = g_firestore.collection("QUIZ").document(categoryId)
                        .collection("TESTS_LIST").document("TESTS_INFO");
                
                testsInfoRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (!documentSnapshot.exists()) {
                                completeListener.onFailure();
                                return;
                            }
                            
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data == null) {
                                completeListener.onFailure();
                                return;
                            }
                            
                            // Find which test number corresponds to this testId
                            int testNumber = -1;
                            for (String key : data.keySet()) {
                                if (key.endsWith("_ID") && testId.equals(data.get(key))) {
                                    try {
                                        testNumber = Integer.parseInt(key.substring(4, key.length() - 3));
                                        break;
                                    } catch (NumberFormatException e) {
                                        Log.e("DbQuery", "Failed to parse test number from key: " + key);
                                    }
                                }
                            }
                            
                            if (testNumber == -1) {
                                completeListener.onFailure();
                                return;
                            }
                            
                            // Get total number of tests
                            int noOfTests = 0;
                            Long noOfTestsLong = documentSnapshot.getLong("NO_OF_TESTS");
                            if (noOfTestsLong != null) {
                                noOfTests = noOfTestsLong.intValue();
                            } else {
                                // Count test entries
                                for (String key : data.keySet()) {
                                    if (key.endsWith("_ID")) {
                                        noOfTests++;
                                    }
                                }
                            }
                            
                            // Create a batch to update all test entries
                            WriteBatch batch = g_firestore.batch();
                            
                            // Remove the test and shift all tests after it
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("NO_OF_TESTS", noOfTests - 1);
                            
                            // Remove the test fields
                            updates.put("TEST" + testNumber + "_ID", FieldValue.delete());
                            updates.put("TEST" + testNumber + "_TIME", FieldValue.delete());
                            updates.put("TEST" + testNumber + "_NAME", FieldValue.delete());
                            
                            // Shift all tests after this one
                            for (int i = testNumber + 1; i <= noOfTests; i++) {
                                String oldIdKey = "TEST" + i + "_ID";
                                String oldTimeKey = "TEST" + i + "_TIME";
                                String oldNameKey = "TEST" + i + "_NAME";
                                
                                String newIdKey = "TEST" + (i - 1) + "_ID";
                                String newTimeKey = "TEST" + (i - 1) + "_TIME";
                                String newNameKey = "TEST" + (i - 1) + "_NAME";
                                
                                // Copy values from old keys to new keys
                                if (data.containsKey(oldIdKey)) {
                                    updates.put(newIdKey, data.get(oldIdKey));
                                    updates.put(oldIdKey, FieldValue.delete());
                                }
                                
                                if (data.containsKey(oldTimeKey)) {
                                    updates.put(newTimeKey, data.get(oldTimeKey));
                                    updates.put(oldTimeKey, FieldValue.delete());
                                }
                                
                                if (data.containsKey(oldNameKey)) {
                                    updates.put(newNameKey, data.get(oldNameKey));
                                    updates.put(oldNameKey, FieldValue.delete());
                                }
                            }
                            
                            // Update TESTS_INFO document
                            batch.update(testsInfoRef, updates);
                            
                            // Update category document with new test count
                            batch.update(g_firestore.collection("QUIZ").document(categoryId), 
                                    "NO_OF_TESTS", noOfTests - 1);
                            
                            // Commit the batch
                            batch.commit()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("DbQuery", "Test deleted successfully: " + testId);
                                        completeListener.onSuccess();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("DbQuery", "Failed to delete test: " + e.getMessage());
                                        completeListener.onFailure();
                                    }
                                });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("DbQuery", "Failed to get TESTS_INFO: " + e.getMessage());
                            completeListener.onFailure();
                        }
                    });
            }
            
            @Override
            public void onFailure() {
                Log.e("DbQuery", "Failed to delete questions for test: " + testId);
                completeListener.onFailure();
            }
        });
    }
    
    // ==================== QUESTION ADMIN OPERATIONS ====================
    
    // Load questions for admin
    public static void loadQuestionsForAdmin(String categoryId, String testId, final MyCompleteListener completeListener) {
        g_adminQuestionList.clear();
        
        // Try different collection names for questions
        g_firestore.collection("Questions")
            .whereEqualTo("CATEGORY", categoryId)
            .whereEqualTo("TEST", testId)
            .get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    int questionNo = 1;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        String question = doc.getString("QUESTION");
                        String optionA = doc.getString("OPTION_A");
                        String optionB = doc.getString("OPTION_B");
                        String optionC = doc.getString("OPTION_C");
                        String optionD = doc.getString("OPTION_D");
                        String correctAnswer = doc.getString("CORRECT_ANS");
                        
                        g_adminQuestionList.add(new QuestionAdminModel(
                            docId,
                            question,
                            optionA,
                            optionB,
                            optionC,
                            optionD,
                            correctAnswer,
                            testId,
                            categoryId
                        ));
                        questionNo++;
                    }
                    
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to load questions for admin: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Add a new question
    public static void addQuestion(String categoryId, String testId, String question, 
                                  String optionA, String optionB, String optionC, String optionD, 
                                  String correctAnswer, final MyCompleteListener completeListener) {
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("CATEGORY", categoryId);
        questionData.put("TEST", testId);
        questionData.put("QUESTION", question);
        questionData.put("OPTION_A", optionA);
        questionData.put("OPTION_B", optionB);
        questionData.put("OPTION_C", optionC);
        questionData.put("OPTION_D", optionD);
        questionData.put("CORRECT_ANS", correctAnswer);
        
        g_firestore.collection("Questions").add(questionData)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    // Update question count in test document
                    updateQuestionCount(categoryId, testId, 1, new MyCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("DbQuery", "Question added successfully");
                            completeListener.onSuccess();
                        }
                        
                        @Override
                        public void onFailure() {
                            Log.e("DbQuery", "Failed to update question count");
                            completeListener.onFailure();
                        }
                    });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to add question: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Update an existing question
    public static void updateQuestion(String questionId, String question, 
                                     String optionA, String optionB, String optionC, String optionD, 
                                     String correctAnswer, final MyCompleteListener completeListener) {
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("QUESTION", question);
        questionData.put("OPTION_A", optionA);
        questionData.put("OPTION_B", optionB);
        questionData.put("OPTION_C", optionC);
        questionData.put("OPTION_D", optionD);
        questionData.put("CORRECT_ANS", correctAnswer);
        
        g_firestore.collection("Questions").document(questionId).update(questionData)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DbQuery", "Question updated successfully");
                    completeListener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to update question: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Delete a question
    public static void deleteQuestion(String categoryId, String testId, String questionId, final MyCompleteListener completeListener) {
        g_firestore.collection("Questions").document(questionId).delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Update question count in test document
                    updateQuestionCount(categoryId, testId, -1, new MyCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("DbQuery", "Question deleted successfully");
                            completeListener.onSuccess();
                        }
                        
                        @Override
                        public void onFailure() {
                            Log.e("DbQuery", "Failed to update question count");
                            completeListener.onFailure();
                        }
                    });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DbQuery", "Failed to delete question: " + e.getMessage());
                    completeListener.onFailure();
                }
            });
    }
    
    // Helper method to update question count for a test
    private static void updateQuestionCount(String categoryId, String testId, int change, final MyCompleteListener completeListener) {
        // First get the current question count
        getQuestionCountForTest(categoryId, testId, new MyCompleteListener() {
            @Override
            public void onSuccess() {
                // g_total_question now contains the current count
                int newCount = g_total_question + change;
                if (newCount < 0) newCount = 0; // Ensure count doesn't go negative
                
                // Update the test model if it exists in the admin list
                for (TestAdminModel test : g_adminTestList) {
                    if (test.getDocID().equals(testId)) {
                        test.setQuestionCount(newCount);
                        break;
                    }
                }
                
                completeListener.onSuccess();
            }
            
            @Override
            public void onFailure() {
                completeListener.onFailure();
            }
        });
    }
}
