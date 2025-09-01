# Quiz App Difficulty Level System

## Overview
This document explains the difficulty level system implemented in your Quiz App. The system automatically unlocks higher difficulty tests based on user performance and completion percentages.

## How It Works

### 1. Difficulty Levels
- **EASY**: Always unlocked (default)
- **MEDIUM**: Unlocks when user completes 70% of the category
- **HARD**: Unlocks when user completes 85% of the category

### 2. Test Unlocking Logic
- Tests are automatically assigned difficulty levels based on their position:
  - Tests 1-3: EASY (always unlocked)
  - Tests 4-6: MEDIUM (unlocks at 70%)
  - Tests 7+: HARD (unlocks at 85%)

### 3. Progress Tracking
- User progress is tracked per category
- Progress is saved both locally and to Firebase
- Test completion percentages are calculated automatically

## Database Configuration

### Firestore Structure
To configure difficulty levels in your Firestore database, add these fields to your `TESTS_INFO` document:

```json
{
  "TEST1_ID": "test1",
  "TEST1_TIME": 30,
  "TEST1_DIFFICULTY": "EASY",
  "TEST1_REQUIRED_SCORE": 0,
  
  "TEST2_ID": "test2", 
  "TEST2_TIME": 30,
  "TEST2_DIFFICULTY": "EASY",
  "TEST2_REQUIRED_SCORE": 0,
  
  "TEST3_ID": "test3",
  "TEST3_TIME": 30,
  "TEST3_DIFFICULTY": "EASY", 
  "TEST3_REQUIRED_SCORE": 0,
  
  "TEST4_ID": "test4",
  "TEST4_TIME": 45,
  "TEST4_DIFFICULTY": "MEDIUM",
  "TEST4_REQUIRED_SCORE": 70,
  
  "TEST5_ID": "test5",
  "TEST5_TIME": 45,
  "TEST5_DIFFICULTY": "MEDIUM",
  "TEST5_REQUIRED_SCORE": 70,
  
  "TEST6_ID": "test6",
  "TEST6_TIME": 45,
  "TEST6_DIFFICULTY": "MEDIUM",
  "TEST6_REQUIRED_SCORE": 70,
  
  "TEST7_ID": "test7",
  "TEST7_TIME": 60,
  "TEST7_DIFFICULTY": "HARD",
  "TEST7_REQUIRED_SCORE": 85
}
```

### Field Descriptions
- `TEST{X}_ID`: Unique identifier for the test
- `TEST{X}_TIME`: Time limit in minutes
- `TEST{X}_DIFFICULTY`: Difficulty level (EASY, MEDIUM, HARD)
- `TEST{X}_REQUIRED_SCORE`: Minimum category completion percentage required

## User Experience Features

### 1. Visual Indicators
- **Unlocked Tests**: Normal appearance with difficulty badge
- **Locked Tests**: Grayed out with lock icon and unlock requirements
- **Difficulty Badges**: Color-coded (Green=EASY, Orange=MEDIUM, Red=HARD)

### 2. Progress Feedback
- Progress bars show individual test scores
- Category completion percentage is tracked
- Unlock requirements are clearly displayed

### 3. Smart Unlocking
- Tests automatically unlock when requirements are met
- No manual intervention needed
- Progress persists across app sessions

## Implementation Details

### Key Classes
1. **UserProgressManager**: Handles progress tracking and test unlocking
2. **TestModel**: Enhanced with difficulty and unlock status
3. **TestAdapter**: Displays tests with visual lock/unlock states
4. **DbQuery**: Loads tests with difficulty information

### Progress Storage
- **Local**: SharedPreferences for fast access
- **Firebase**: Firestore for cross-device sync
- **Automatic**: Progress updates after each test completion

## Customization Options

### 1. Change Unlock Thresholds
Edit the `UserProgressManager.java` file:

```java
// Medium tests unlock at 70% category completion
if (difficulty.equals("MEDIUM")) {
    return categoryCompletion >= 70; // Change this value
} else if (difficulty.equals("HARD")) {
    return categoryCompletion >= 85; // Change this value
}
```

### 2. Add More Difficulty Levels
1. Add new difficulty constants to `TestModel.java`
2. Update `getDifficultyColor()` method
3. Modify unlock logic in `UserProgressManager.java`

### 3. Custom Difficulty Assignment
Override the automatic difficulty assignment in `DbQuery.java`:

```java
// Custom difficulty logic
if (testDifficulty == null) {
    // Your custom logic here
    testDifficulty = determineCustomDifficulty(testId, categoryId);
}
```

## Testing the System

### 1. Reset Progress
```java
UserProgressManager progressManager = UserProgressManager.getInstance(context);
progressManager.resetProgress();
```

### 2. Check Test Status
```java
boolean isUnlocked = testModel.isUnlocked();
String difficulty = testModel.getDifficulty();
int requiredScore = testModel.getRequiredScore();
```

### 3. Monitor Progress
```java
int categoryProgress = progressManager.getCategoryCompletion(categoryId);
Map<String, Integer> allProgress = progressManager.getAllCategoryProgress();
```

## Benefits

1. **Progressive Learning**: Users must master basics before advanced content
2. **Engagement**: Clear goals and unlockable content
3. **Adaptive**: System adjusts to user skill level
4. **Motivation**: Visual progress and achievement system
5. **Scalability**: Easy to add more tests and difficulty levels

## Troubleshooting

### Common Issues
1. **Tests not unlocking**: Check Firebase data structure and field names
2. **Progress not saving**: Verify Firebase authentication and permissions
3. **UI not updating**: Ensure adapter is notified of data changes

### Debug Information
Enable logging to see detailed progress information:
```java
Log.d("UserProgressManager", "Category completion: " + categoryCompletion);
Log.d("TestModel", "Test unlock status: " + test.isUnlocked());
```

## Future Enhancements

1. **Dynamic Difficulty**: Adjust based on user performance
2. **Skill Trees**: Multiple progression paths
3. **Achievement System**: Badges and rewards
4. **Social Features**: Compare progress with friends
5. **Analytics**: Detailed performance insights

---

This difficulty system provides a solid foundation for progressive learning while maintaining user engagement through clear goals and visual feedback.
