const en = {

    common: {
        appName: 'Adaptive Learning',
        loading: 'Loading...',
        save: 'Save',
        cancel: 'Cancel',
        delete: 'Delete',
        edit: 'Edit',
        create: 'Create',
        search: 'Search',
        filter: 'Filter',
        export: 'Export',
        import: 'Import',
        back: 'Back',
        next: 'Next',
        previous: 'Previous',
        submit: 'Submit',
        confirm: 'Confirm',
        close: 'Close',
        yes: 'Yes',
        no: 'No',
        all: 'All',
        none: 'None',
        select: 'Select',
        noData: 'No data',
        actions: 'Actions',
        details: 'Details',
        status: 'Status',
        date: 'Date',
        time: 'Time',
        description: 'Description',
        name: 'Name',
        type: 'Type',
        success: 'Success',
        error: 'Error',
        warning: 'Warning',
        info: 'Information',
    },

    // ============ AUTH ============
    auth: {
        login: 'Login',
        logout: 'Logout',
        email: 'Email',
        password: 'Password',
        forgotPassword: 'Forgot password?',
        rememberMe: 'Remember me',
        loginButton: 'Sign in',
        loggingIn: 'Signing in...',
        loginSuccess: 'Successfully logged in!',
        loginError: 'Invalid email or password',
        logoutSuccess: 'Successfully logged out',
        sessionExpired: 'Session expired, please log in again',
        unauthorized: 'You do not have access to this page',
    },

    // ============ ROLES ============
    roles: {
        admin: 'Administrator',
        teacher: 'Teacher',
        student: 'Student',
    },

    // ============ GROUPS ============
    groups: {
        experimental: 'Experimental',
        control: 'Control',
        experimentalShort: 'EXP',
        controlShort: 'CTRL',
    },

    // ============ NAVIGATION ============
    nav: {
        dashboard: 'Dashboard',
        courses: 'Courses',
        classes: 'Classes',
        tasks: 'Tasks',
        exams: 'Exams',
        students: 'Students',
        teachers: 'Teachers',
        progress: 'Progress',
        analytics: 'Analytics',
        surveys: 'Surveys',
        settings: 'Settings',
        profile: 'Profile',
        help: 'Help',
    },

    // ============ DASHBOARD ============
    dashboard: {
        welcome: 'Welcome, {{name}}!',
        welcomeBack: 'Welcome back, {{name}}!',
        lastLogin: 'Last login: {{date}}',
        quickActions: 'Quick Actions',
        recentActivity: 'Recent Activity',
        upcomingTasks: 'Upcoming Tasks',
        noUpcomingTasks: 'No upcoming tasks',

        // Student specific
        continuelearning: 'Continue Learning',
        practiceRecommendation: 'Practice Recommended',
        daysSinceLastPractice: 'Days since last practice: {{days}}',
        skillsToFocus: 'Skills to Focus On',

        // Teacher specific
        pendingSubmissions: 'Pending Submissions',
        classOverview: 'Class Overview',
        commonIssues: 'Common Issues',
    },

    // ============ CLASSES ============
    classes: {
        title: 'Classes',
        addClass: 'Add Class',
        editClass: 'Edit Class',
        deleteClass: 'Delete Class',
        className: 'Class Name',
        schoolYear: 'School Year',
        studentCount: 'Student Count',
        experimentalCount: 'Experimental Group',
        controlCount: 'Control Group',
        importStudents: 'Import Students',
        importFromCsv: 'Import from CSV (e-Gradebook)',
        randomizeGroups: 'Randomize Groups',
        randomizeConfirm: 'Are you sure you want to randomly assign students to experimental and control groups?',
        randomizeSuccess: 'Students successfully assigned to groups',
        viewStudents: 'View Students',
        noClasses: 'No classes created',
    },

    // ============ COURSES ============
    courses: {
        title: 'Courses',
        addCourse: 'Add Course',
        editCourse: 'Edit Course',
        deleteCourse: 'Delete Course',
        courseName: 'Course Name',
        courseCode: 'Course Code',
        language: 'Programming Language',
        outcomes: 'Learning Outcomes',
        addOutcome: 'Add Outcome',
        editOutcome: 'Edit Outcome',
        outcomeCode: 'Outcome Code',
        outcomeDescription: 'Outcome Description',
        skills: 'Skills',
        noCourses: 'No courses created',
        noOutcomes: 'No outcomes defined',
    },

    // ============ TASKS ============
    tasks: {
        title: 'Tasks',
        addTask: 'Add Task',
        editTask: 'Edit Task',
        deleteTask: 'Delete Task',
        taskTitle: 'Task Title',
        taskDescription: 'Task Description',
        taskType: 'Task Type',
        difficulty: 'Difficulty',
        timeLimit: 'Time Limit (min)',
        availableFrom: 'Available From',
        availableTo: 'Available To',
        testCases: 'Test Cases',
        addTestCase: 'Add Test Case',
        input: 'Input',
        expectedOutput: 'Expected Output',
        hiddenTest: 'Hidden Test',
        gradingCriteria: 'Grading Criteria',
        addCriterion: 'Add Criterion',
        criterion: 'Criterion',
        maxPoints: 'Max Points',
        weight: 'Weight',
        starterCode: 'Starter Code',
        expectedSolution: 'Expected Solution',
        generateTask: 'Generate Task (AI)',
        noTasks: 'No tasks created',

        // Task types
        types: {
            practice: 'Practice',
            homework: 'Homework',
            exam: 'Exam',
        },

        // Difficulty
        difficulties: {
            1: 'Very Easy',
            2: 'Easy',
            3: 'Medium',
            4: 'Hard',
            5: 'Very Hard',
        },
    },

    // ============ EXAMS ============
    exams: {
        title: 'Exams',
        addExam: 'Add Exam',
        editExam: 'Edit Exam',
        deleteExam: 'Delete Exam',
        examTitle: 'Exam Title',
        duration: 'Duration (min)',
        totalPoints: 'Total Points',
        selectTasks: 'Select Tasks',
        assignToClasses: 'Assign to Classes',
        noExams: 'No exams created',
        startExam: 'Start Exam',
        endExam: 'End Exam',
        timeRemaining: 'Time Remaining',
        examInProgress: 'Exam in Progress',
        examCompleted: 'Exam Completed',
    },

    // ============ SUBMISSIONS ============
    submissions: {
        title: 'Submissions',
        submitCode: 'Submit Code',
        submitting: 'Submitting...',
        submitSuccess: 'Code submitted successfully!',
        submitError: 'Error submitting code',
        viewSubmission: 'View Submission',
        mySubmissions: 'My Submissions',
        allSubmissions: 'All Submissions',
        compileSuccess: 'Compilation successful',
        compileError: 'Compilation error',
        compileErrors: 'Compilation Errors',
        testResults: 'Test Results',
        testsPassed: 'Tests Passed',
        memoryAnalysis: 'Memory Analysis',
        memoryLeaks: 'Memory leaks detected',
        noMemoryLeaks: 'No memory leaks',
        aiFeedback: 'AI Feedback',
        teacherFeedback: 'Teacher Feedback',
        addFeedback: 'Add Feedback',
        score: 'Score',
        noSubmissions: 'No submissions',
    },

    // ============ EDITOR ============
    editor: {
        title: 'Code Editor',
        run: 'Run',
        running: 'Running...',
        reset: 'Reset',
        output: 'Output',
        input: 'Input',
        console: 'Console',
        clearConsole: 'Clear Console',
        fontSize: 'Font Size',
        theme: 'Theme',
        darkTheme: 'Dark',
        lightTheme: 'Light',
    },

    // ============ PROGRESS & MASTERY ============
    progress: {
        title: 'Progress',
        myProgress: 'My Progress',
        studentProgress: 'Student Progress',
        classProgress: 'Class Progress',
        skillMastery: 'Skill Mastery',
        masteryLevel: 'Mastery Level',
        totalAttempts: 'Total Attempts',
        correctAttempts: 'Correct Attempts',
        lastPracticed: 'Last Practiced',
        trend: 'Trend',

        // Mastery levels
        mastery: {
            notStarted: 'Not Started',
            beginner: 'Beginner',
            intermediate: 'Intermediate',
            advanced: 'Advanced',
            mastered: 'Mastered',
        },

        // Trends
        trends: {
            improving: 'Improving',
            stable: 'Stable',
            declining: 'Declining',
        },
    },

    // ============ ANALYTICS ============
    analytics: {
        title: 'Analytics',
        classStatistics: 'Class Statistics',
        groupComparison: 'Group Comparison',
        weeklyProgress: 'Weekly Progress',
        commonIssues: 'Common Issues',
        affectedStudents: 'Affected Students',
        occurrences: 'Occurrences',
        avgMastery: 'Average Mastery',
        avgScore: 'Average Score',
        submissionCount: 'Submission Count',
        completionRate: 'Completion Rate',
        experimentalGroup: 'Experimental Group',
        controlGroup: 'Control Group',
        difference: 'Difference',
        significanceLevel: 'Significance Level',
        strengths: 'Strengths',
        weaknesses: 'Weaknesses',
        recommendations: 'Recommendations',
    },

    // ============ NOTES ============
    notes: {
        title: 'Notes',
        teacherNotes: 'Teacher Notes',
        aiNotes: 'AI Notes',
        addNote: 'Add Note',
        editNote: 'Edit Note',
        deleteNote: 'Delete Note',
        importNotes: 'Import Notes',
        importFromCsv: 'Import from CSV (e-Gradebook)',
        noteContent: 'Note Content',
        source: 'Source',
        sources: {
            aiGenerated: 'AI Generated',
            teacherManual: 'Teacher',
            csvImport: 'CSV Import',
        },
        noNotes: 'No notes',
    },

    // ============ SURVEYS ============
    surveys: {
        title: 'Surveys',
        addSurvey: 'Create Survey',
        editSurvey: 'Edit Survey',
        deleteSurvey: 'Delete Survey',
        surveyTitle: 'Survey Title',
        targetAudience: 'Target Audience',
        questions: 'Questions',
        addQuestion: 'Add Question',
        questionText: 'Question Text',
        questionType: 'Question Type',
        options: 'Options',
        active: 'Active',
        inactive: 'Inactive',
        responses: 'Responses',
        viewResponses: 'View Responses',
        submitSurvey: 'Submit Survey',
        surveyCompleted: 'Survey completed successfully!',
        noSurveys: 'No active surveys',

        // Question types
        questionTypes: {
            likert5: 'Likert Scale (1-5)',
            likert7: 'Likert Scale (1-7)',
            text: 'Text Response',
            multipleChoice: 'Multiple Choice',
        },

        // Likert labels
        likert5: {
            1: 'Strongly Disagree',
            2: 'Disagree',
            3: 'Neither Agree nor Disagree',
            4: 'Agree',
            5: 'Strongly Agree',
        },
    },

    // ============ USERS ============
    users: {
        title: 'Users',
        addUser: 'Add User',
        editUser: 'Edit User',
        deleteUser: 'Delete User',
        firstName: 'First Name',
        lastName: 'Last Name',
        role: 'Role',
        class: 'Class',
        group: 'Group',
        enrolledCourses: 'Enrolled Courses',
        createdAt: 'Created At',
        noUsers: 'No users',
    },

    // ============ CSV IMPORT ============
    csv: {
        importTitle: 'Import from CSV',
        selectFile: 'Select File',
        dragDrop: 'Drag and drop file here',
        or: 'or',
        browse: 'Browse',
        importing: 'Importing...',
        importSuccess: 'Successfully imported {{count}} records',
        importError: 'Import error',
        totalRows: 'Total Rows',
        importedRows: 'Imported Rows',
        errors: 'Errors',
        row: 'Row',
        field: 'Field',
        message: 'Message',
    },

    // ============ LANGUAGES ============
    languages: {
        title: 'Language',
        hr: 'Croatian',
        en: 'English',
        c: 'C',
        csharp: 'C#',
    },

    // ============ ERRORS ============
    errors: {
        general: 'An error occurred',
        notFound: 'Page not found',
        serverError: 'Server error',
        networkError: 'Network error',
        validationError: 'Please check your input',
        required: 'This field is required',
        invalidEmail: 'Please enter a valid email address',
        minLength: 'Minimum length is {{min}} characters',
        maxLength: 'Maximum length is {{max}} characters',
        invalidFormat: 'Invalid format',
        fileTooLarge: 'File is too large',
        invalidFileType: 'Unsupported file type',
    },

    // ============ CONFIRMATIONS ============
    confirmations: {
        delete: 'Are you sure you want to delete?',
        deleteWarning: 'This action cannot be undone.',
        logout: 'Are you sure you want to log out?',
        unsavedChanges: 'You have unsaved changes. Do you want to leave this page?',
        randomizeGroups: 'This will randomly assign students to groups. Do you want to continue?',
    },
};

export default en;