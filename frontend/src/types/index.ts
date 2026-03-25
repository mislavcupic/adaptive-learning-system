// ==================== ENUMS ====================

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';
export type GroupType = 'CONTROL' | 'EXPERIMENTAL';
export type LanguageType = 'C' | 'CSHARP';
export type SubmissionStatus = 
    | 'PENDING' 
    | 'COMPILING' 
    | 'RUNNING' 
    | 'COMPLETED' 
    | 'COMPILE_ERROR' 
    | 'RUNTIME_ERROR' 
    | 'TIMEOUT' 
    | 'FAILED';

// ==================== API RESPONSE ====================

export interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export interface PaginatedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

// ==================== AUTH ====================

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    role?: UserRole;
    groupType?: GroupType;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    user: User;
}

// ==================== USER ====================

export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: UserRole;
    groupType: GroupType | null;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface Student extends User {
    role: 'STUDENT';
    classId?: string;
    className?: string;
    averageMastery?: number;
    totalSubmissions?: number;
}

export interface Teacher extends User {
    role: 'TEACHER';
}

export interface CsvImportResult {
    imported: number;
    skipped: number;
    errors: string[];
}

// ==================== SCHOOL CLASS ====================

export interface SchoolClass {
    id: string;
    name: string;
    description: string | null;
    academicYear: string;
    teacherId: string;
    teacherName: string;
    studentCount: number;
    courseCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface ClassStatistics {
    totalStudents: number;
    experimentalCount: number;
    controlCount: number;
    averageMastery: number;
    averageScore: number;
    totalSubmissions: number;
    completionRate: number;
}

// ==================== COURSE ====================

export interface Course {
    id: string;
    name: string;
    description: string | null;
    languageType: LanguageType;
    isActive: boolean;
    createdById: string;
    createdByName: string;
    outcomeCount: number;
    createdAt: string;
    updatedAt: string;
}

// ==================== LEARNING OUTCOME ====================

export interface LearningOutcome {
    id: string;
    name: string;
    description: string | null;
    orderIndex: number;
    courseId: string;
    courseName: string;
    taskCount: number;
    createdAt: string;
    updatedAt: string;
}

// ==================== TASK ====================

export interface Task {
    id: string;
    title: string;
    description: string | null;
    instructions: string;
    starterCode: string | null;
    solutionCode: string | null;
    testCases: string | null;
    gradingCriteria: string | null;
    maxScore: number;
    timeLimitSeconds: number;
    memoryLimitMb: number;
    outcomeId: string;
    outcomeName: string;
    courseId: string;
    courseName: string;
    languageType: LanguageType;
    createdById: string;
    createdByName: string;
    submissionCount: number;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface Exam {
    id: string;
    title: string;
    description: string | null;
    courseId: string;
    courseName: string;
    taskIds: string[];
    duration: number;
    startTime: string;
    endTime: string;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

// ==================== SUBMISSION ====================

export interface Submission {
    id: string;
    code: string;
    status: SubmissionStatus;
    score: number | null;
    compilerOutput: string | null;
    executionOutput: string | null;
    executionTimeMs: number | null;
    memoryUsedKb: number | null;
    testCasesPassed: number;
    testCasesTotal: number;
    aiFeedback: string | null;
    teacherFeedback: string | null;
    studentId: string;
    studentName: string;
    taskId: string;
    taskTitle: string;
    createdAt: string;
    updatedAt: string;
}

// ==================== SKILL MASTERY ====================

export interface SkillMastery {
    id: string;
    skillName: string;
    masteryLevel: number;
    totalAttempts: number;
    successfulAttempts: number;
    pTransit: number;
    pGuess: number;
    pSlip: number;
    studentId: string;
    studentName: string;
    createdAt: string;
    updatedAt: string;
}

// ==================== NOTES ====================

export interface TeacherNote {
    id: string;
    studentId: string;
    teacherId: string;
    teacherName: string;
    note: string;
    createdAt: string;
    updatedAt: string;
}

export interface AnalyticNote {
    id: string;
    insight: string;
    context: string | null;
    studentId: string;
    studentName: string;
    submissionId: string | null;
    createdAt: string;
}

// ==================== SURVEY ====================

export interface Survey {
    id: string;
    title: string;
    description: string | null;
    questions: SurveyQuestion[];
    isActive: boolean;
    createdById: string;
    createdAt: string;
    updatedAt: string;
}

export interface SurveyQuestion {
    id: string;
    text: string;
    type: 'TEXT' | 'RATING' | 'MULTIPLE_CHOICE' | 'CHECKBOX';
    options?: string[];
    required: boolean;
}

export interface SurveyResponse {
    id: string;
    surveyId: string;
    userId: string;
    answers: Record<string, unknown>;
    createdAt: string;
}

// ==================== REMINDERS ====================

export interface StudentReminder {
    pendingTasks: number;
    upcomingDeadlines: number;
    unreadFeedback: number;
    tasks: Task[];
}

// ==================== ANALYTICS ====================

export interface StudentDetailedProgress {
    studentId: string;
    studentName: string;
    averageMastery: number;
    totalSubmissions: number;
    completedTasks: number;
    pendingTasks: number;
    skillMasteries: SkillMastery[];
    recentSubmissions: Submission[];
}

export interface CommonIssue {
    id: string;
    description: string;
    frequency: number;
    affectedStudents: number;
    skillName: string;
}

export interface WeeklyProgress {
    week: string;
    submissions: number;
    averageScore: number;
    averageMastery: number;
}

// ==================== DASHBOARD ====================

export interface StudentDashboardData {
    student: User;
    totalSubmissions: number;
    completedTasks: number;
    pendingTasks: number;
    averageMastery: number;
    recentSubmissions: Submission[];
    skillMasteries: SkillMastery[];
    enrolledCourses: Course[];
}

export interface TeacherDashboardData {
    teacher: User;
    totalStudents: number;
    totalCourses: number;
    totalTasks: number;
    totalSubmissions: number;
    pendingReviews: number;
    recentSubmissions: Submission[];
    studentProgress: StudentProgressSummary[];
    courses: Course[];
}

export interface StudentProgressSummary {
    studentId: string;
    studentName: string;
    averageMastery: number;
    totalSubmissions: number;
    lastActivity: string;
}

export interface AdminDashboardData {
    totalUsers: number;
    totalStudents: number;
    totalTeachers: number;
    totalAdmins: number;
    activeUsers: number;
    totalCourses: number;
    activeCourses: number;
    totalClasses: number;
    totalTasks: number;
    totalSubmissions: number;
    systemHealth: SystemHealth;
}

export interface SystemHealth {
    databaseStatus: 'UP' | 'DOWN';
    mlServiceStatus: 'UP' | 'DOWN';
    codeExecutorStatus: 'UP' | 'DOWN';
}
