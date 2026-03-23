export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';
export type GroupType = 'EXPERIMENTAL' | 'CONTROL';
export type LanguageType = 'C' | 'CSHARP';
export type TaskType = 'PRACTICE' | 'HOMEWORK' | 'EXAM';
export type NoteSource = 'AI_GENERATED' | 'TEACHER_MANUAL' | 'CSV_IMPORT';
export type TrendType = 'IMPROVING' | 'STABLE' | 'DECLINING';
export type QuestionType = 'LIKERT_5' | 'LIKERT_7' | 'TEXT' | 'MULTIPLE_CHOICE';
export type SurveyTargetRole = 'STUDENT' | 'TEACHER' | 'ALL';

// ============ KORISNICI ============
export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: UserRole;
    createdAt: string;
}

export interface Student extends User {
    role: 'STUDENT';
    classId: string;
    className?: string;
    groupType: GroupType;
    enrolledCourseIds: string[];
}

export interface Teacher extends User {
    role: 'TEACHER';
    courseIds: string[];
}

export interface Admin extends User {
    role: 'ADMIN';
}

export interface SchoolClass {
    id: string;
    name: string;
    schoolYear: string;
    teacherId: string;
    studentCount: number;
    experimentalCount: number;
    controlCount: number;
    createdAt: string;
}


export interface Course {
    id: string;
    name: string;
    code: string;
    description: string;
    languageType: LanguageType;
    teacherId: string;
    classIds: string[];
    createdAt: string;
}

export interface LearningOutcome {
    id: string;
    courseId: string;
    code: string;
    description: string;
    skills: string[];
    orderIndex: number;
}

export interface GradingCriteria {
    id: string;
    taskId: string;
    criterion: string;
    maxPoints: number;
    weight: number;
}


export interface TestCase {
    id: string;
    input: string;
    expectedOutput: string;
    isHidden: boolean;
    points: number;
}

export interface Task {
    id: string;
    outcomeId: string;
    title: string;
    description: string;
    taskType: TaskType;
    difficulty: 1 | 2 | 3 | 4 | 5;
    starterCode?: string;
    expectedSolution?: string;
    testCases: TestCase[];
    gradingCriteria: GradingCriteria[];
    timeLimitMinutes?: number;
    availableFrom?: string;
    availableTo?: string;
    createdAt: string;
}

export interface Exam {
    id: string;
    courseId: string;
    title: string;
    description: string;
    taskIds: string[];
    totalPoints: number;
    durationMinutes: number;
    availableFrom: string;
    availableTo: string;
    classIds: string[];
    createdAt: string;
}


export interface TestResult {
    testCaseId: string;
    passed: boolean;
    actualOutput: string;
    executionTimeMs: number;
    memoryUsedKb?: number;
}

export interface ValgrindReport {
    hasMemoryLeaks: boolean;
    leakSummary?: string;
    errors: string[];
}

export interface AIAnalysis {
    codeQuality: number;
    correctness: number;
    efficiency: number;
    style: number;
    identifiedIssues: string[];
    suggestions: string[];
}

export interface Submission {
    id: string;
    studentId: string;
    taskId: string;
    submittedCode: string;
    compileSuccess: boolean;
    compileErrors?: string;
    testResults: TestResult[];
    valgrindReport?: ValgrindReport;
    aiAnalysis?: AIAnalysis;
    aiFeedback?: string;
    aiScore?: number;
    teacherFeedback?: string;
    teacherScore?: number;
    createdAt: string;
}


export interface SkillMastery {
    id: string;
    studentId: string;
    skillName: string;
    masteryLevel: number;
    pTransit: number;
    pGuess: number;
    pSlip: number;
    totalAttempts: number;
    correctAttempts: number;
    lastPracticed: string;
    trend: TrendType;
}


export interface AnalyticNote {
    id: string;
    studentId: string;
    skillTag: string;
    insight: string;
    source: NoteSource;
    createdAt: string;
}

export interface TeacherNote {
    id: string;
    teacherId: string;
    studentId: string;
    note: string;
    isFromCsv: boolean;
    createdAt: string;
}


export interface SurveyQuestion {
    id: string;
    surveyId: string;
    questionText: string;
    questionType: QuestionType;
    options?: string[];
    orderIndex: number;
}

export interface Survey {
    id: string;
    title: string;
    description: string;
    targetRole: SurveyTargetRole;
    questions: SurveyQuestion[];
    isActive: boolean;
    createdBy: string;
    availableFrom: string;
    availableTo: string;
    createdAt: string;
}

export interface SurveyAnswer {
    questionId: string;
    value: string | number;
}

export interface SurveyResponse {
    id: string;
    surveyId: string;
    userId: string;
    userRole: UserRole;
    groupType?: GroupType;
    answers: SurveyAnswer[];
    submittedAt: string;
}


export interface CommonIssue {
    skillName: string;
    occurrenceCount: number;
    affectedStudentCount: number;
    percentage: number;
    exampleErrors: string[];
}

export interface WeeklyProgress {
    weekStart: string;
    avgMastery: number;
    submissionCount: number;
    experimentalAvg: number;
    controlAvg: number;
}

export interface ClassStatistics {
    classId: string;
    className: string;
    totalStudents: number;
    experimentalCount: number;
    controlCount: number;
    averageMastery: number;
    commonIssues: CommonIssue[];
    experimentalAvgMastery: number;
    controlAvgMastery: number;
    weeklyProgress: WeeklyProgress[];
}

export interface StudentDetailedProgress {
    student: Student;
    skills: SkillMastery[];
    recentSubmissions: Submission[];
    strengths: string[];
    weaknesses: string[];
    recommendations: string[];
    progressTrend: TrendType;
}


export interface StudentReminder {
    id: string;
    studentId: string;
    message: string;
    skillsToFocus: string[];
    suggestedTaskIds: string[];
    lastLoginAt: string;
    daysSinceLastPractice: number;
}


export interface CsvImportError {
    row: number;
    field: string;
    message: string;
}

export interface CsvImportResult {
    success: boolean;
    totalRows: number;
    importedRows: number;
    errors: CsvImportError[];
}


export interface LoginCredentials {
    email: string;
    password: string;
}

export interface AuthResponse {
    user: User;
    accessToken: string;
    refreshToken: string;
}

export interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}


export interface PaginatedResponse<T> {
    data: T[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
}

export interface ApiErrorResponse {
    message: string;
    errors?: Record<string, string[]>;
}