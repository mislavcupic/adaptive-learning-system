import type { TaskType } from './index';

export type ExamAttemptStatus =
    | 'IN_PROGRESS'
    | 'SUBMITTED'
    | 'EXPIRED'
    | 'GRADED'
    | 'REVIEWED';

export interface ExamTask {
    taskId: string;
    title: string;
    description: string | null;
    instructions: string | null;
    taskType: TaskType;
    options: string | null;
    starterCode: string | null;
    maxScore: number | null;
    timeLimitSeconds: number | null;
    languageType: string | null;
    orderIndex: number | null;
}

export interface Exam {
    id: string;
    title: string;
    description: string | null;
    instructions: string | null;
    courseId: string;
    courseName: string;
    languageType: string | null;
    createdByName: string | null;
    timeLimitMinutes: number | null;
    passingScore: number | null;
    maxScore: number | null;
    taskCount: number;
    availableFrom: string | null;
    availableUntil: string | null;
    showTestResults: boolean;
    isPublished: boolean;
    isActive: boolean;
    createdAt: string;
    tasks?: ExamTask[];
    myAttemptStatus?: ExamAttemptStatus | null;
    myAttemptId?: string | null;
}

export interface ExamRequest {
    title: string;
    description?: string;
    instructions?: string;
    courseId: string;
    taskIds?: string[];
    timeLimitMinutes?: number | null;
    passingScore?: number;
    availableFrom?: string | null;
    availableUntil?: string | null;
    showTestResults?: boolean;
    isPublished?: boolean;
}

export interface ExamAnswerEntry {
    taskId: string;
    taskTitle: string;
    taskType: TaskType;
    answerContent: string | null;
    markedDone: boolean;
    answeredAt: string | null;
    aiScore: number | null;
    teacherScore: number | null;
    finalScore: number | null;
    maxScore: number | null;
    teacherFeedback: string | null;
    testsPassed: number | null;
    testsTotal: number | null;
    compilerOutput: string | null;
    aiFeedback: string | null;
}

export interface ExamAttempt {
    id: string;
    examId: string;
    examTitle: string;
    studentId: string;
    studentName: string;
    studentEmail: string;
    status: ExamAttemptStatus;
    startedAt: string;
    submittedAt: string | null;
    deadlineAt: string | null;
    remainingSeconds: number | null;
    totalScore: number | null;
    maxScore: number | null;
    percentage: number | null;
    passed: boolean | null;
    teacherFeedback: string | null;
    reviewedAt: string | null;
    answers?: ExamAnswerEntry[];
}

export interface ExamAnswerRequest {
    taskId: string;
    answerContent?: string;
    markedDone?: boolean;
    runTests?: boolean;
}