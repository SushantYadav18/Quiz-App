package com.example.quizpractice;

import android.util.ArrayMap;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbQuery {

    public static FirebaseFirestore g_firestore = FirebaseFirestore.getInstance();
    public static List<CategoryModel> g_catList = new ArrayList<>();
    public static List<TestModel> g_testList = new ArrayList<>();
    public static int g_selected_cat_index = 0;

    public static ProfileModel myProfile = new ProfileModel("NA" ,"null");
    public static int g_selected_test_index = 0;
  public static  List<QuestionModel> g_questionList = new ArrayList<>();

    public static void createUserData( String name, String email, MyCompleteListener completeListener) {


        Map<String, Object> userData = new ArrayMap<>();
        userData.put("EMAIL_ID", email);
        userData.put("NAME", name);
        userData.put("TOTAL_SCORE", 0);


       DocumentReference userDoc = g_firestore.collection("USERS").document(FirebaseAuth.getInstance().getCurrentUser().getUid());

        WriteBatch batch = g_firestore.batch();

        batch.set(userDoc, userData);
        DocumentReference countDoc = g_firestore.collection("USERS").document("TOTAL_USERS");

        batch.update(countDoc, "COUNT", FieldValue.increment(1));
        batch.commit()
        .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                completeListener.onSuccess();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure( @NonNull Exception e) {
                completeListener.onFailure();
                    }
                });


    }

    public  static  void  getUserData(final MyCompleteListener completeListener){
        g_firestore.collection("USERS").document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.d("DbQuery", "User document data: " + documentSnapshot.getData());
                        myProfile.setName(documentSnapshot.getString("NAME"));
                        myProfile.setEmail(documentSnapshot.getString("EMAIL_ID"));
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
                                String testIdKey = "TEST" + i + "_ID";
                                String testTimeKey = "TEST" + i + "_TIME";
                                
                                String testId = documentSnapshot.getString(testIdKey);
                                Long testTime = documentSnapshot.getLong(testTimeKey);

                                Log.d("DbQuery", "Checking test " + i + ":");
                                Log.d("DbQuery", "  - " + testIdKey + " = " + testId);
                                Log.d("DbQuery", "  - " + testTimeKey + " = " + testTime);

                                if (testId != null && testTime != null) {
                                    Log.d("DbQuery", "Adding test: " + testId + " with time: " + testTime);
                                    g_testList.add(new TestModel(
                                        testId,
                                        0, // Initial score
                                        testTime.intValue()
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
}

