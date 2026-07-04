import { apiClient, ENDPOINTS } from '../api';
import type {
    Assessment,
    AssessmentAttempt,
    AssessmentAttemptRequest,
    AssessmentRequest,
    QuestionRequest
} from '../types';

export const assessmentService = {
    getByCourse: async (courseId: string): Promise<Assessment[]> => {
        return await apiClient.get<Assessment[]>(
            ENDPOINTS.ASSESSMENTS.BY_COURSE(courseId)
        );
    },

    getById: async (id: string): Promise<Assessment> => {
        return await apiClient.get<Assessment>(
            ENDPOINTS.ASSESSMENTS.BY_ID(id)
        );
    },

    create: async (data: AssessmentRequest): Promise<Assessment> => {
        return await apiClient.post<Assessment>(
            ENDPOINTS.ASSESSMENTS.BASE,
            data
        );
    },

    addQuestion: async (data: QuestionRequest): Promise<Assessment> => {
        return await apiClient.post<Assessment>(
            ENDPOINTS.ASSESSMENTS.QUESTIONS,
            data
        );
    },

    deleteQuestion: async (questionId: string): Promise<void> => {
        await apiClient.delete<void>(
            ENDPOINTS.ASSESSMENTS.DELETE_QUESTION(questionId)
        );
    },

    startAttempt: async (assessmentId: string): Promise<AssessmentAttempt> => {
        return await apiClient.post<AssessmentAttempt>(
            ENDPOINTS.ASSESSMENTS.START(assessmentId)
        );
    },

    submitAttempt: async (data: AssessmentAttemptRequest): Promise<AssessmentAttempt> => {
        return await apiClient.post<AssessmentAttempt>(
            ENDPOINTS.ASSESSMENTS.SUBMIT,
            data
        );
    },

    getMyAttempts: async (): Promise<AssessmentAttempt[]> => {
        return await apiClient.get<AssessmentAttempt[]>(
            ENDPOINTS.ASSESSMENTS.MY_ATTEMPTS
        );
    },

    getAttempt: async (attemptId: string): Promise<AssessmentAttempt> => {
        return await apiClient.get<AssessmentAttempt>(
            ENDPOINTS.ASSESSMENTS.ATTEMPT_BY_ID(attemptId)
        );
    },

    hasCompletedPretest: async (courseId: string): Promise<boolean> => {
        return await apiClient.get<boolean>(
            ENDPOINTS.ASSESSMENTS.CHECK_PRETEST(courseId)
        );
    },

    hasCompletedPosttest: async (courseId: string): Promise<boolean> => {
        return await apiClient.get<boolean>(
            ENDPOINTS.ASSESSMENTS.CHECK_POSTTEST(courseId)
        );
    },
};