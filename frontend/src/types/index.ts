

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

//api

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

//auth

export interface LoginCredentials {
    email: string;
    password: string;
    [key: string]: unknown; // Dodaj ovo
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    role?: UserRole;
    groupType?: GroupType;
    [key: string]: unknown; // I ovo
}
export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    user: User;
}

//user

export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: UserRole;
    groupType?: GroupType | null;
    isActive: boolean;
    createdAt: string;
    updatedAt?: string;
}

// ============================================================================
// SCHOOL CLASS
// ============================================================================

export interface SchoolClass {
    id: string;
    name: string;
    description?: string | null;
    academicYear: string;
    teacherId: string;
    teacherName?: string;
    studentCount?: number;
    courseCount?: number;
    createdAt: string;
    updatedAt?: string;
}

export interface SchoolClassRequest {
    name: string;
    description?: string;
    academicYear: string;
}

//course

export interface Course {
    id: string;
    name: string;
    description?: string | null;
    languageType: LanguageType;
    isActive: boolean;
    createdById?: string;
    createdByName?: string;
    outcomeCount?: number;
    createdAt: string;
    updatedAt?: string;
}

export interface CourseRequest {
    name: string;
    description?: string;
    languageType: LanguageType;
}

//learning outcome

export interface LearningOutcome {
    id: string;
    name: string;
    description?: string | null;
    orderIndex: number;
    courseId: string;
    courseName?: string;
    taskCount?: number;
    createdAt: string;
    updatedAt?: string;
}

export interface LearningOutcomeRequest {
    name: string;
    description?: string;
    orderIndex: number;
    courseId: string;
}

//task

export interface Task {
    id: string;
    title: string;
    description?: string | null;
    instructions: string;
    starterCode?: string | null;
    solutionCode?: string | null;
    testCases?: string | null;
    gradingCriteria?: string | null;
    maxScore: number;
    timeLimitSeconds: number;
    memoryLimitMb: number;
    outcomeId: string;
    outcomeName?: string;
    courseId?: string;
    courseName?: string;
    languageType?: LanguageType;
    createdById?: string;
    createdByName?: string;
    submissionCount?: number;
    isActive: boolean;
    dueDate?: string | null;
    createdAt: string;
    updatedAt?: string;
}

export interface TaskRequest {
    title: string;
    description?: string;
    instructions: string;
    starterCode?: string;
    solutionCode?: string;
    testCases?: string;
    gradingCriteria?: string;
    maxScore: number;
    timeLimitSeconds: number;
    memoryLimitMb: number;
    outcomeId: string;
    dueDate?: string;
}

//subm

export interface Submission {
    id: string;
    code: string;
    status: SubmissionStatus;
    score?: number | null;
    compilerOutput?: string | null;
    executionOutput?: string | null;
    executionTimeMs?: number | null;
    memoryUsedKb?: number | null;
    testCasesPassed: number;
    testCasesTotal: number;
    aiFeedback?: string | null;
    teacherFeedback?: string | null;
    studentId: string;
    studentName?: string;
    taskId: string;
    taskTitle?: string;
    createdAt: string;
    updatedAt?: string;
}

export interface SubmissionRequest {
    taskId: string;
    code: string;
}

//skillmaster

export interface SkillMastery {
    id: string;
    skillName: string;
    masteryLevel: number;
    totalAttempts: number;
    successfulAttempts: number;
    pInit?: number;
    pTransit?: number;
    pGuess?: number;
    pSlip?: number;
    studentId: string;
    studentName?: string;
    createdAt: string;
    updatedAt?: string;
}

//dash

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
    studentProgress: StudentProgress[];
    courses: Course[];
}

export interface StudentProgress {
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
    databaseStatus: string;
    mlServiceStatus: string;
    codeExecutorStatus: string;
}


export interface ValidationError {
    field: string;
    message: string;
}

export interface FormState<T> {
    data: T;
    errors: Record<string, string>;
    isSubmitting: boolean;
    isValid: boolean;
}
