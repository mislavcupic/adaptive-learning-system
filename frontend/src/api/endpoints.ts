export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const ENDPOINTS = {
    AUTH: {
        LOGIN: '/auth/login',
        REGISTER: '/auth/register',
        LOGOUT: '/auth/logout',
        REFRESH: '/auth/refresh',
        ME: '/auth/me',
    },
    DASHBOARD: {
        STUDENT: '/dashboard/student',
        TEACHER: '/dashboard/teacher',
        ADMIN: '/dashboard/admin',
    },
    USERS: {
        BASE: '/users',
        BY_ID: (id: string) => `/users/${id}`,
        STUDENTS: '/users/students',
        TEACHERS: '/users/teachers',
        ACTIVATE: (id: string) => `/users/${id}/activate`,
        DEACTIVATE: (id: string) => `/users/${id}/deactivate`,
    },
    CLASSES: {
        BASE: '/classes',
        BY_ID: (id: string) => `/classes/${id}`,
        STUDENTS: (id: string) => `/classes/${id}/students`,
        ADD_STUDENT: (id: string, studentId: string) => `/classes/${id}/students/${studentId}`,
    },
    COURSES: {
        BASE: '/courses',
        BY_ID: (id: string) => `/courses/${id}`,
        OUTCOMES: (id: string) => `/courses/${id}/outcomes`,
        ACTIVATE: (id: string) => `/courses/${id}/activate`,
        DEACTIVATE: (id: string) => `/courses/${id}/deactivate`,
    },
    OUTCOMES: {
        BASE: '/outcomes',
        BY_ID: (id: string) => `/outcomes/${id}`,
        TASKS: (id: string) => `/outcomes/${id}/tasks`,
    },
    TASKS: {
        BASE: '/tasks',
        BY_ID: (id: string) => `/tasks/${id}`,
        BY_COURSE: (courseId: string) => `/tasks/course/${courseId}`,
    },
    SUBMISSIONS: {
        BASE: '/submissions',
        BY_ID: (id: string) => `/submissions/${id}`,
        SUBMIT: '/student/submit',
        MY: '/submissions/my',
        BY_TASK: (taskId: string) => `/submissions/task/${taskId}`,
        BY_STUDENT: (studentId: string) => `/submissions/student/${studentId}`,
        FEEDBACK: (id: string) => `/submissions/${id}/feedback`,
    },
    SKILLS: {
        BY_STUDENT: (studentId: string) => `/skills/student/${studentId}`,
    },
} as const;
