# Test Unlock System Implementation

## Overview
This document explains the implementation of the test unlock system where:
- TestA is set as EASY difficulty and is always unlocked
- TestB is set as MEDIUM difficulty and is only unlocked if TestA is completed with at least 75% success rate
- TestC is set as HARD difficulty and is only unlocked if TestA is completed with at least 75% success rate

## Implementation Details

### 1. UserProgressManager Changes
The `shouldUnlockTest` method in `UserProgressManager.java` has been modified to implement the specific unlock requirements:

```java
public boolean shouldUnlockTest(String categoryId, String testId, String difficulty, int requiredScore) {
    // TestA is always unlocked (EASY difficulty)
    if (testId.equals("A") || difficulty.equals("EASY")) {
        return true;
    }
    
    // TestB is unlocked only if TestA is completed with at least 75% success rate
    if (testId.equals("B") || difficulty.equals("MEDIUM")) {
        int testACompletion = getTestCompletion(categoryId, "A");
        return testACompletion >= 75;
    }
    
    // TestC is unlocked only if TestB is unlocked (which requires TestA to be completed with 75%)
    if (testId.equals("C") || difficulty.equals("HARD")) {
        int testACompletion = getTestCompletion(categoryId, "A");
        return testACompletion >= 75; // If TestA is completed with 75%, then TestB and TestC are unlocked
    }
    
    // Default rules for other tests based on difficulty
    // ...
}
```

### 2. TestAdapter Changes
The `TestAdapter.java` file has been updated to display the specific unlock requirements for TestB and TestC:

```java
// Set unlock requirement text
String unlockText = "";
if (model.getId().equals("B") || model.getDifficulty().equals("MEDIUM")) {
    unlockText = "Complete TestA with 75% score to unlock";
} else if (model.getId().equals("C") || model.getDifficulty().equals("HARD")) {
    unlockText = "Complete TestA with 75% score to unlock";
} else if (model.getDifficulty().equals("MEDIUM")) {
    unlockText = "Complete 70% of category to unlock";
} else if (model.getDifficulty().equals("HARD")) {
    unlockText = "Complete 85% of category to unlock";
}
holderUnlockText.setText(unlockText);
```

### 3. TestModel Changes
The `TestModel.java` constructor has been updated to ensure TestA is always unlocked by default:

```java
public TestModel(String id, int topScore, int time, String difficulty, int requiredScore) {
    // ...
    // TestA is always unlocked by default
    this.isUnlocked = id.equals("A") || difficulty.equals("EASY");
}
```

## Testing the Implementation

1. TestA should always be accessible to users
2. TestB should only be accessible if the user has completed TestA with a score of 75% or higher
3. TestC should only be accessible if the user has completed TestA with a score of 75% or higher

## Database Configuration

Ensure your Firestore database has the correct test configurations:

```json
{
  "TEST_A_ID": "A",
  "TEST_A_TIME": 30,
  "TEST_A_DIFFICULTY": "EASY",
  "TEST_A_REQUIRED_SCORE": 0,
  
  "TEST_B_ID": "B", 
  "TEST_B_TIME": 45,
  "TEST_B_DIFFICULTY": "MEDIUM",
  "TEST_B_REQUIRED_SCORE": 75,
  
  "TEST_C_ID": "C",
  "TEST_C_TIME": 60,
  "TEST_C_DIFFICULTY": "HARD",
  "TEST_C_REQUIRED_SCORE": 75
}
```