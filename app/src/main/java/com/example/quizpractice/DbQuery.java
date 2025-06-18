package com.example.quizpractice;

import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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

    public static FirebaseFirestore g_firestore;
    public static List<CategoryModel> g_catList = new ArrayList<>();



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

    public static void loadCategories(final MyCompleteListener completeListener) {
        g_catList.clear();
        g_firestore.collection("QUIZ").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        try {
                            Log.d("DbQuery", "Total documents found: " + queryDocumentSnapshots.size());
                            
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                // Skip the Categories document if it exists
                                if (doc.getId().equals("Categories")) {
                                    continue;
                                }

                                String catName = doc.getString("NAME");
                                Long noOfTests = doc.getLong("NO_OF_TESTS");
                                
                                if (catName != null && noOfTests != null) {
                                    Log.d("DbQuery", "Adding category: " + catName + " with " + noOfTests + " tests");
                                    g_catList.add(new CategoryModel(doc.getId(), catName, noOfTests.intValue()));
                                } else {
                                    Log.e("DbQuery", "Missing NAME or NO_OF_TESTS for document: " + doc.getId());
                                }
                            }

                            Log.d("DbQuery", "Total categories loaded: " + g_catList.size());
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

    private static void createDefaultCategories(final MyCompleteListener completeListener) {
        Log.d("DbQuery", "Creating default categories");
        // Create default categories
        WriteBatch batch = g_firestore.batch();
        
        // Create category documents directly in QUIZ collection
        DocumentReference cat1Ref = g_firestore.collection("QUIZ").document("CAT1");
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("NAME", "Mathematics");
        cat1.put("NO_OF_TESTS", 5);
        batch.set(cat1Ref, cat1);

        DocumentReference cat2Ref = g_firestore.collection("QUIZ").document("CAT2");
        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("NAME", "Science");
        cat2.put("NO_OF_TESTS", 5);
        batch.set(cat2Ref, cat2);

        DocumentReference cat3Ref = g_firestore.collection("QUIZ").document("CAT3");
        Map<String, Object> cat3 = new HashMap<>();
        cat3.put("NAME", "History");
        cat3.put("NO_OF_TESTS", 5);
        batch.set(cat3Ref, cat3);

        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d("DbQuery", "Default categories created successfully");
                // After creating default categories, load them
                loadCategories(completeListener);
            })
            .addOnFailureListener(e -> {
                Log.e("DbQuery", "Failed to create default categories: " + e.getMessage());
                e.printStackTrace();
                completeListener.onFailure();
            });
    }
}
