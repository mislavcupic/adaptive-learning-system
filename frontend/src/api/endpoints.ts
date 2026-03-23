export const ENDPOINTS = {
    // Auth
    AUTH: {
        LOGIN: '/auth/login',
        REGISTER: '/auth/register',
        REFRESH: '/auth/refresh',
        LOGOUT: '/auth/logout',
        ME: '/auth/me',
    },

    // Users
    USERS: {
        BASE: '/users',
        BY_ID: (id: string) => `/users/${id}`,
        STUDENTS: '/users/students',
        TEACHERS: '/users/teachers',
        IMPORT_CSV: '/users/import/csv',
    },

    // Classes
    CLASSES: {
        BASE: '/classes',
        BY_ID: (id: string) => `/classes/${id}`,
        STUDENTS: (id: string) => `/classes/${id}/students`,
        RANDOMIZE_GROUPS: (id: string) => `/classes/${id}/randomize-groups`,
        STATISTICS: (id: string) => `/classes/${id}/statistics`,
    },

    // Courses
    COURSES: {
        BASE: '/courses',
        BY_ID: (id: string) => `/courses/${id}`,
        OUTCOMES: (courseId: string) => `/courses/${courseId}/outcomes`,
        OUTCOME_BY_ID: (courseId: string, outcomeId: string) =>
            `/courses/${courseId}/outcomes/${outcomeId}`,
    },

    // Tasks
    TASKS: {
        BASE: '/tasks',
        BY_ID: (id: string) => `/tasks/${id}`,
        BY_OUTCOME: (outcomeId: string) => `/tasks/outcome/${outcomeId}`,
        GENERATE: '/tasks/generate', // AI generiranje zadataka
    },

    // Exams
    EXAMS: {
        BASE: '/exams',
        BY_ID: (id: string) => `/exams/${id}`,
        BY_COURSE: (courseId: string) => `/exams/course/${courseId}`,
    },

    // Submissions
    SUBMISSIONS: {
        BASE: '/submissions',
        BY_ID: (id: string) => `/submissions/${id}`,
        SUBMIT: '/submissions/submit',
        BY_STUDENT: (studentId: string) => `/submissions/student/${studentId}`,
        BY_TASK: (taskId: string) => `/submissions/task/${taskId}`,
    },

    // Skills & Progress
    SKILLS: {
        BY_STUDENT: (studentId: string) => `/skills/student/${studentId}`,
        MASTERY: (studentId: string, skillName: string) =>
            `/skills/student/${studentId}/mastery/${skillName}`,
    },

    // Analytics
    ANALYTICS: {
        CLASS_STATS: (classId: string) => `/analytics/class/${classId}`,
        STUDENT_PROGRESS: (studentId: string) => `/analytics/student/${studentId}`,
        COMMON_ISSUES: (classId: string) => `/analytics/class/${classId}/issues`,
        WEEKLY_PROGRESS: (classId: string) => `/analytics/class/${classId}/weekly`,
        COMPARE_GROUPS: (classId: string) => `/analytics/class/${classId}/compare`,
    },

    // Notes
    NOTES: {
        TEACHER: '/notes/teacher',
        TEACHER_BY_STUDENT: (studentId: string) => `/notes/teacher/student/${studentId}`,
        IMPORT_CSV: '/notes/import/csv',
        ANALYTIC: (studentId: string) => `/notes/analytic/student/${studentId}`,
    },

    // Surveys
    SURVEYS: {
        BASE: '/surveys',
        BY_ID: (id: string) => `/surveys/${id}`,
        ACTIVE: '/surveys/active',
        RESPOND: (id: string) => `/surveys/${id}/respond`,
        RESPONSES: (id: string) => `/surveys/${id}/responses`,
        ANALYTICS: (id: string) => `/surveys/${id}/analytics`,
    },

    // Reminders
    REMINDERS: {
        BY_STUDENT: (studentId: string) => `/reminders/student/${studentId}`,
    },
} as const;