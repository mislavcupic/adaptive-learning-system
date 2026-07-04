export type AssessmentType = 'PRETEST' | 'POSTTEST';
export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODE';

export interface Assessment {
    id: string;
    title: string;
    description?: string;
    assessmentType: AssessmentType;
    courseId: string;
    courseName: string;
    timeLimitMinutes?: number;
    passingScore: number;
    questionCount: number;
    questions?: Question[];
}

export interface Question {
    id: string;
    questionText: string;
    questionType: QuestionType;
    options?: string;
    codeTemplate?: string;
    points: number;
    orderIndex: number;
}

export interface AssessmentAttempt {
    id: string;
    assessmentId: string;
    assessmentTitle: string;
    assessmentType: AssessmentType;
    startedAt: string;
    completedAt?: string;
    score: number;
    maxScore: number;
    percentage: number;
    isCompleted: boolean;
    passed: boolean;
}

export interface AssessmentAttemptRequest {
    assessmentId: string;
    answers: Record<string, string>;
}

export interface AssessmentRequest {
    title: string;
    description?: string;
    assessmentType: AssessmentType;
    courseId: string;
    timeLimitMinutes?: number;
    passingScore?: number;
}

export interface QuestionRequest {
    assessmentId: string;
    questionText: string;
    questionType: QuestionType;
    options?: string;
    correctAnswer: string;
    codeTemplate?: string;
    testCases?: string;
    points?: number;
    orderIndex?: number;
}