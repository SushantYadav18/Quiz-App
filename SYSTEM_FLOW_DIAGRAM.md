# Quiz App System Flow Diagram

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Components](#architecture-components)
3. [User Authentication Flow](#user-authentication-flow)
4. [Main Application Flow](#main-application-flow)
5. [Quiz Taking Flow](#quiz-taking-flow)
6. [Progress & Difficulty System](#progress--difficulty-system)
7. [Data Flow Architecture](#data-flow-architecture)
8. [Firebase Integration](#firebase-integration)
9. [Session Management](#session-management)
10. [Component Interaction Diagram](#component-interaction-diagram)

---

## System Overview

```mermaid
graph TB
    A[User] --> B[SplashActivity]
    B --> C{Session Valid?}
    C -->|Yes| D[MainActivity]
    C -->|No| E[LoginActivity]
    E --> F{Authentication}
    F -->|Success| G[Load User Data]
    F -->|Failure| E
    G --> D
    D --> H[CategoryFragment]
    D --> I[LeaderBoardFragment]
    D --> J[AccountFragment]
    H --> K[TestActivity]
    K --> L[QuestionsActivity]
    L --> M[ResultActivity]
    M --> N[Save Results to Firebase]
```

---

## Architecture Components

### Core Activities
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  SplashActivity │───▶│  LoginActivity  │───▶│  MainActivity   │
│                 │    │                 │    │                 │
│ • App Init      │    │ • Email/Pass    │    │ • Navigation    │
│ • Session Check │    │ • Google Auth   │    │ • Fragment Host │
│ • Auto Login    │    │ • User Creation │    │ • Drawer Menu   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐    ┌─────────────────┐
                       │  TestActivity   │◀───│ CategoryFragment│
                       │                 │    │                 │
                       │ • Test List     │    │ • Category Grid │
                       │ • Unlock Status │    │ • Progress Stats│
                       │ • Difficulty    │    │ • Sample Data   │
                       └─────────────────┘    └─────────────────┘
                               │
                       ┌─────────────────┐    ┌─────────────────┐
                       │QuestionsActivity│───▶│ ResultActivity  │
                       │                 │    │                 │
                       │ • Quiz Interface│    │ • Score Display │
                       │ • Timer System  │    │ • Progress Save │
                       │ • Navigation    │    │ • Firebase Sync │
                       └─────────────────┘    └─────────────────┘
```

### Core Fragments
```
MainActivity
├── CategoryFragment (Home)
│   ├── CategoryAdapter
│   ├── CategoryModel
│   └── Grid Layout (2 columns)
├── LeaderBoardFragment
│   ├── LeaderboardAdapter
│   └── RankModel
└── AccountFragment
    ├── ProfileModel
    └── User Settings
```

### Core Systems
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ SessionManager  │    │UserProgressMgr  │    │    DbQuery      │
│                 │    │                 │    │                 │
│ • Login State   │    │ • Test Unlocks  │    │ • Firebase Ops  │
│ • 24hr Timeout  │    │ • Difficulty    │    │ • Data Loading  │
│ • Auto Refresh  │    │ • Progress Track│    │ • CRUD Ops      │
│ • Session Logs  │    │ • Score Calc    │    │ • Error Handle  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## User Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant S as SplashActivity
    participant SM as SessionManager
    participant L as LoginActivity
    participant F as Firebase Auth
    participant M as MainActivity

    U->>S: Launch App
    S->>SM: Check Session
    alt Session Valid
        SM->>S: Valid Session
        S->>M: Navigate to Main
    else No Valid Session
        SM->>S: Invalid/No Session
        S->>L: Navigate to Login
        U->>L: Enter Credentials
        alt Email/Password
            L->>F: signInWithEmailAndPassword()
            F->>L: Authentication Result
        else Google Sign-In
            L->>F: signInWithCredential()
            F->>L: Authentication Result
        end
        alt Success
            L->>SM: createSession()
            L->>DbQuery: loadData()
            L->>M: Navigate to Main
        else Failure
            L->>U: Show Error
        end
    end
```

---

## Main Application Flow

```mermaid
flowchart TD
    A[MainActivity] --> B{Bottom Navigation}
    B -->|Home| C[CategoryFragment]
    B -->|Leaderboard| D[LeaderBoardFragment]
    B -->|Profile| E[AccountFragment]
    
    C --> F[Load Categories]
    F --> G{Categories Available?}
    G -->|Yes| H[Display Category Grid]
    G -->|No| I[Add Sample Categories]
    I --> H
    
    H --> J[User Selects Category]
    J --> K[TestActivity]
    
    K --> L[Load Tests for Category]
    L --> M[UserProgressManager]
    M --> N[Update Test Unlock Status]
    N --> O[Display Test List]
    
    O --> P[User Selects Test]
    P --> Q{Test Unlocked?}
    Q -->|Yes| R[QuestionsActivity]
    Q -->|No| S[Show Unlock Requirements]
    
    R --> T[Load Questions]
    T --> U{Questions Available?}
    U -->|Yes| V[Start Quiz]
    U -->|No| W[Add Sample Questions]
    W --> V
```

---

## Quiz Taking Flow

```mermaid
sequenceDiagram
    participant U as User
    participant QA as QuestionsActivity
    participant QAd as QuestionAdapter
    participant T as Timer
    participant RA as ResultActivity
    participant DB as DbQuery

    U->>QA: Start Quiz
    QA->>QA: setupQuestionData()
    QA->>T: startTimer()
    QA->>QAd: Initialize with Questions
    
    loop Quiz Session
        U->>QAd: Answer Question
        QAd->>QAd: markQuestionAsVisited()
        U->>QA: Navigate Questions
        QA->>QA: updateQuestionCounter()
    end
    
    alt Time Up
        T->>QA: onFinish()
        QA->>QA: submitQuiz()
    else User Submits
        U->>QA: Submit Button
        QA->>QA: showSubmitConfirmation()
        U->>QA: Confirm Submit
        QA->>QA: submitQuiz()
    end
    
    QA->>QAd: calculateScore()
    QAd->>QA: Return Score
    QA->>RA: Navigate with Results
    RA->>DB: saveResult()
    DB->>Firebase: Update User Progress
```

---

## Progress & Difficulty System

```mermaid
graph TB
    A[Test Selection] --> B[UserProgressManager]
    B --> C{Check Test ID}
    
    C -->|TestA or EASY| D[Always Unlocked]
    C -->|TestB or MEDIUM| E{TestA >= 75%?}
    C -->|TestC or HARD| F{TestA >= 75%?}
    C -->|Other Tests| G{Check Difficulty}
    
    E -->|Yes| H[Unlock TestB]
    E -->|No| I[Show Requirement: Complete TestA with 75%]
    
    F -->|Yes| J[Unlock TestC]
    F -->|No| K[Show Requirement: Complete TestA with 75%]
    
    G -->|MEDIUM| L{Category >= 70%?}
    G -->|HARD| M{Category >= 85%?}
    
    L -->|Yes| N[Unlock Medium Test]
    L -->|No| O[Show Requirement: Complete 70% of category]
    
    M -->|Yes| P[Unlock Hard Test]
    M -->|No| Q[Show Requirement: Complete 85% of category]
    
    subgraph "Test Completion Flow"
        R[Quiz Completed] --> S[Calculate Score %]
        S --> T[Save to SharedPreferences]
        T --> U[Save to Firebase]
        U --> V[Update Category Progress]
        V --> W[Recalculate Unlock Status]
    end
```

### Difficulty Level Rules
```
EASY Tests (1-3):
├── Always unlocked
├── No prerequisites
└── Entry point for users

MEDIUM Tests (4-6):
├── Unlock at 70% category completion
├── OR TestA completed with 75%+ (for TestB)
└── Intermediate difficulty

HARD Tests (7+):
├── Unlock at 85% category completion
├── OR TestA completed with 75%+ (for TestC)
└── Advanced difficulty
```

---

## Data Flow Architecture

```mermaid
flowchart LR
    subgraph "Local Storage"
        A[SharedPreferences]
        B[Session Data]
        C[Progress Cache]
    end
    
    subgraph "Application Layer"
        D[Activities/Fragments]
        E[Adapters]
        F[Models]
    end
    
    subgraph "Business Logic"
        G[DbQuery]
        H[SessionManager]
        I[UserProgressManager]
    end
    
    subgraph "Firebase Backend"
        J[Authentication]
        K[Firestore Database]
        L[Session Logs]
    end
    
    D <--> G
    E <--> F
    G <--> K
    H <--> A
    H <--> J
    I <--> C
    I <--> K
    G <--> L
```

### Database Structure
```
Firestore Collections:
├── USERS/{userId}
│   ├── NAME: String
│   ├── EMAIL_ID: String
│   ├── TOTAL_SCORE: Number
│   └── PROGRESS/{categoryId}
│       ├── CATEGORY_ID: String
│       ├── OVERALL_COMPLETION: Number
│       ├── TOTAL_TESTS: Number
│       └── TEST_{testId}: Object
│           ├── SCORE: Number
│           ├── MAX_SCORE: Number
│           ├── PERCENTAGE: Number
│           └── COMPLETED_AT: Timestamp
├── QUIZ/{categoryId}
│   ├── NAME: String
│   ├── NO_OF_TESTS: Number
│   └── TESTS_LIST/TESTS_INFO
│       ├── TEST1_ID: String
│       ├── TEST1_TIME: Number
│       ├── TEST1_DIFFICULTY: String
│       ├── TEST1_REQUIRED_SCORE: Number
│       └── ... (repeat for each test)
├── Questions/{questionId}
│   ├── CATEGORY: String
│   ├── TEST: String
│   ├── QUESTION: String
│   ├── A, B, C, D: String (options)
│   └── ANSWER: Number (correct option index)
└── SESSION_LOGS/{logId}
    ├── userId: String
    ├── sessionId: String
    ├── event: String
    ├── timestamp: Number
    └── duration: Number
```

---

## Firebase Integration

```mermaid
sequenceDiagram
    participant App as Application
    participant Auth as Firebase Auth
    participant FS as Firestore
    participant SM as SessionManager
    participant UPM as UserProgressManager

    Note over App,UPM: Authentication Flow
    App->>Auth: signInWithEmailAndPassword()
    Auth->>App: FirebaseUser
    App->>SM: createSession()
    SM->>FS: Log session start

    Note over App,UPM: Data Loading Flow
    App->>FS: Load categories
    FS->>App: Category data
    App->>FS: Load tests for category
    FS->>App: Test data
    App->>UPM: Update unlock status
    UPM->>FS: Check user progress

    Note over App,UPM: Quiz Completion Flow
    App->>UPM: saveTestCompletion()
    UPM->>FS: Save test result
    UPM->>UPM: Update category progress
    UPM->>FS: Update overall progress
    FS->>App: Confirmation
```

### Firebase Operations
```
DbQuery Class Methods:
├── Authentication
│   ├── createUserData()
│   └── getUserData()
├── Categories
│   └── loadCategories()
├── Tests
│   └── loadTests()
├── Questions
│   └── loadquestions()
├── Results
│   └── saveResult()
└── Utilities
    └── debugDatabaseStructure()
```

---

## Session Management

```mermaid
stateDiagram-v2
    [*] --> NoSession
    NoSession --> LoginRequired: App Launch
    LoginRequired --> Authenticating: User Login
    Authenticating --> ActiveSession: Success
    Authenticating --> LoginRequired: Failure
    ActiveSession --> ActiveSession: Activity (refresh)
    ActiveSession --> SessionExpired: 24hr timeout
    ActiveSession --> LoggedOut: User logout
    SessionExpired --> LoginRequired: Auto redirect
    LoggedOut --> LoginRequired: Clear data
    LoginRequired --> [*]: App Exit
```

### Session Features
```
SessionManager Capabilities:
├── Session Creation
│   ├── Generate unique session ID
│   ├── Store user credentials
│   └── Log to Firebase
├── Session Validation
│   ├── 24-hour timeout
│   ├── Auto-refresh on activity
│   └── Firebase sync
├── Session Termination
│   ├── Manual logout
│   ├── Auto-expiry
│   └── Data cleanup
└── Session Analytics
    ├── Duration tracking
    ├── Activity logs
    └── Usage statistics
```

---

## Component Interaction Diagram

```mermaid
graph TB
    subgraph "UI Layer"
        A[Activities]
        B[Fragments]
        C[Adapters]
    end
    
    subgraph "Business Logic"
        D[DbQuery]
        E[SessionManager]
        F[UserProgressManager]
    end
    
    subgraph "Data Models"
        G[CategoryModel]
        H[TestModel]
        I[QuestionModel]
        J[ProfileModel]
        K[RankModel]
    end
    
    subgraph "External Services"
        L[Firebase Auth]
        M[Firestore]
        N[Google Sign-In]
    end
    
    A --> D
    B --> D
    C --> G
    C --> H
    C --> I
    D --> L
    D --> M
    E --> L
    E --> M
    F --> M
    A --> E
    A --> F
    B --> F
    D --> G
    D --> H
    D --> I
    D --> J
    D --> K
    L --> N
```

### Key Interactions
```
MainActivity ←→ SessionManager: Session validation
CategoryFragment ←→ DbQuery: Load categories
TestActivity ←→ UserProgressManager: Check unlock status
QuestionsActivity ←→ DbQuery: Load questions
ResultActivity ←→ DbQuery: Save results
All Components ←→ Firebase: Data persistence
```

---

## Error Handling & Edge Cases

### Error Handling Strategy
```mermaid
flowchart TD
    A[Operation Start] --> B{Try Operation}
    B -->|Success| C[Continue Flow]
    B -->|Network Error| D[Show Retry Option]
    B -->|Auth Error| E[Redirect to Login]
    B -->|Data Error| F[Use Sample Data]
    B -->|Unknown Error| G[Log & Show Generic Error]
    
    D --> H{User Retries?}
    H -->|Yes| B
    H -->|No| I[Graceful Degradation]
    
    F --> J[Notify User of Sample Data]
    J --> C
    
    G --> K[Report to Analytics]
    K --> I
```

### Edge Cases Handled
```
Authentication:
├── No internet connection
├── Invalid credentials
├── Google Sign-In failure
└── Session expiry during use

Data Loading:
├── Empty database collections
├── Malformed data structures
├── Missing required fields
└── Network timeouts

Quiz Taking:
├── No questions available
├── Timer synchronization
├── App backgrounding
└── Unexpected exits

Progress Tracking:
├── Concurrent quiz attempts
├── Data sync conflicts
├── Offline mode handling
└── Progress corruption
```

---

## Performance Considerations

### Optimization Strategies
```
Data Loading:
├── Lazy loading of questions
├── Category caching
├── Progress caching in SharedPreferences
└── Batch Firebase operations

UI Performance:
├── RecyclerView optimization
├── Image loading optimization
├── Fragment lifecycle management
└── Memory leak prevention

Network Optimization:
├── Offline capability
├── Request batching
├── Connection pooling
└── Retry mechanisms
```

---

## Security Implementation

```mermaid
flowchart LR
    A[User Input] --> B[Input Validation]
    B --> C[Authentication Check]
    C --> D[Authorization Verification]
    D --> E[Firebase Security Rules]
    E --> F[Data Access]
    
    G[Session Management] --> H[Token Validation]
    H --> I[Timeout Enforcement]
    I --> J[Secure Storage]
```

### Security Features
```
Authentication Security:
├── Firebase Authentication
├── Google OAuth integration
├── Session timeout (24 hours)
└── Secure token storage

Data Security:
├── Firestore security rules
├── User data isolation
├── Input validation
└── SQL injection prevention

Session Security:
├── Unique session IDs
├── Activity tracking
├── Auto-logout on timeout
└── Secure preferences storage
```

---

This system flow diagram provides a comprehensive overview of your Quiz App's architecture, showing how all components interact to create a seamless user experience with robust progress tracking and difficulty management systems.
