// ============================================
// CONSTANTS - App konstante
// ============================================

// ============ ROLE LABELS ============
export const ROLE_LABELS = {
    ADMIN: 'Administrator',
    TEACHER: 'Nastavnik',
    STUDENT: 'Učenik',
} as const;

// ============ GROUP LABELS ============
export const GROUP_LABELS = {
    EXPERIMENTAL: 'Eksperimentalna',
    CONTROL: 'Kontrolna',
} as const;

// ============ TASK TYPE LABELS ============
export const TASK_TYPE_LABELS = {
    PRACTICE: 'Vježba',
    HOMEWORK: 'Domaća zadaća',
    EXAM: 'Ispit',
} as const;

// ============ DIFFICULTY LABELS ============
export const DIFFICULTY_LABELS = {
    1: 'Vrlo lako',
    2: 'Lako',
    3: 'Srednje',
    4: 'Teško',
    5: 'Vrlo teško',
} as const;

// ============ TREND LABELS ============
export const TREND_LABELS = {
    IMPROVING: 'Napreduje',
    STABLE: 'Stabilno',
    DECLINING: 'Opada',
} as const;

// ============ LANGUAGE LABELS ============
export const LANGUAGE_LABELS = {
    C: 'C',
    CSHARP: 'C#',
} as const;

// ============ ROUTES ============
export const ROUTES = {
    // Public
    LOGIN: '/login',

    // Student
    STUDENT_DASHBOARD: '/student',
    STUDENT_TASK: '/student/task/:taskId',
    STUDENT_PROGRESS: '/student/progress',
    STUDENT_COURSES: '/student/courses',

    // Teacher
    TEACHER_DASHBOARD: '/teacher',
    TEACHER_CLASSES: '/teacher/classes',
    TEACHER_CLASS_DETAIL: '/teacher/classes/:classId',
    TEACHER_COURSES: '/teacher/courses',
    TEACHER_COURSE_DETAIL: '/teacher/courses/:courseId',
    TEACHER_TASKS: '/teacher/tasks',
    TEACHER_TASK_CREATE: '/teacher/tasks/create',
    TEACHER_TASK_EDIT: '/teacher/tasks/:taskId/edit',
    TEACHER_STUDENT_PROGRESS: '/teacher/students/:studentId',
    TEACHER_ANALYTICS: '/teacher/analytics',

    // Admin
    ADMIN_DASHBOARD: '/admin',
    ADMIN_USERS: '/admin/users',
    ADMIN_SURVEYS: '/admin/surveys',
    ADMIN_SURVEY_CREATE: '/admin/surveys/create',
    ADMIN_SURVEY_RESULTS: '/admin/surveys/:surveyId/results',
} as const;

// ============ MASTERY THRESHOLDS ============
export const MASTERY_THRESHOLDS = {
    NOT_STARTED: 0,
    BEGINNER: 0.3,
    INTERMEDIATE: 0.6,
    ADVANCED: 0.85,
    MASTERED: 0.95,
} as const;

export const MASTERY_LABELS = {
    NOT_STARTED: 'Nije započeto',
    BEGINNER: 'Početnik',
    INTERMEDIATE: 'Srednji',
    ADVANCED: 'Napredni',
    MASTERED: 'Savladano',
} as const;

// ============ COLORS ============
export const COLORS = {
    primary: '#6366f1',    // Indigo
    secondary: '#8b5cf6',  // Violet
    success: '#22c55e',    // Green
    warning: '#f59e0b',    // Amber
    danger: '#ef4444',     // Red
    info: '#3b82f6',       // Blue

    experimental: '#8b5cf6', // Violet za EXP grupu
    control: '#6366f1',      // Indigo za CONTROL grupu

    mastery: {
        notStarted: '#6b7280',
        beginner: '#ef4444',
        intermediate: '#f59e0b',
        advanced: '#3b82f6',
        mastered: '#22c55e',
    },
} as const;

// ============ PAGINATION ============
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

// ============ EDITOR ============
export const EDITOR_SETTINGS = {
    defaultLanguage: 'c',
    theme: 'vs-dark',
    fontSize: 14,
    tabSize: 4,
    minimap: false,
    automaticLayout: true,
} as const;

// ============ LIKERT SCALE ============
export const LIKERT_5_OPTIONS = [
    { value: 1, label: 'Uopće se ne slažem' },
    { value: 2, label: 'Ne slažem se' },
    { value: 3, label: 'Niti se slažem niti ne slažem' },
    { value: 4, label: 'Slažem se' },
    { value: 5, label: 'U potpunosti se slažem' },
] as const;

export const LIKERT_7_OPTIONS = [
    { value: 1, label: 'Izrazito se ne slažem' },
    { value: 2, label: 'Ne slažem se' },
    { value: 3, label: 'Donekle se ne slažem' },
    { value: 4, label: 'Niti se slažem niti ne slažem' },
    { value: 5, label: 'Donekle se slažem' },
    { value: 6, label: 'Slažem se' },
    { value: 7, label: 'Izrazito se slažem' },
] as const;