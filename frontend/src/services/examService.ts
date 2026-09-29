import { apiClient, ENDPOINTS } from '../api';
import type {
    ApiResponse,
    Exam,
    ExamRequest,
    ExamAttempt,
    ExamAnswerRequest,
} from '../types';

export const examService = {

    // ---------- Nastavnik ----------

    create: async (data: ExamRequest): Promise<Exam> => {
        const response = await apiClient.post<ApiResponse<Exam>>(
            ENDPOINTS.EXAMS.BASE, data
        );
        return response.data;
    },

    update: async (id: string, data: ExamRequest): Promise<Exam> => {
        const response = await apiClient.put<ApiResponse<Exam>>(
            ENDPOINTS.EXAMS.BY_ID(id), data
        );
        return response.data;
    },

    getById: async (id: string): Promise<Exam> => {
        const response = await apiClient.get<ApiResponse<Exam>>(
            ENDPOINTS.EXAMS.BY_ID(id)
        );
        return response.data;
    },

    getByCourse: async (courseId: string): Promise<Exam[]> => {
        const response = await apiClient.get<ApiResponse<Exam[]>>(
            ENDPOINTS.EXAMS.BY_COURSE(courseId)
        );
        return response.data;
    },

    getMyExams: async (): Promise<Exam[]> => {
        const response = await apiClient.get<ApiResponse<Exam[]>>(
            ENDPOINTS.EXAMS.MY
        );
        return response.data;
    },

    publish: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.EXAMS.PUBLISH(id));
    },

    unpublish: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.EXAMS.UNPUBLISH(id));
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.EXAMS.BY_ID(id));
    },

    getAttempts: async (examId: string): Promise<ExamAttempt[]> => {
        const response = await apiClient.get<ApiResponse<ExamAttempt[]>>(
            ENDPOINTS.EXAMS.ATTEMPTS(examId)
        );
        return response.data;
    },

    getAttempt: async (attemptId: string): Promise<ExamAttempt> => {
        const response = await apiClient.get<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.ATTEMPT_BY_ID(attemptId)
        );
        return response.data;
    },

    gradeAnswer: async (
        attemptId: string,
        taskId: string,
        score: number | null,
        feedback: string
    ): Promise<ExamAttempt> => {
        const response = await apiClient.patch<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.GRADE_ANSWER(attemptId, taskId),
            { score, feedback }
        );
        return response.data;
    },

    finalizeReview: async (attemptId: string, feedback: string): Promise<ExamAttempt> => {
        const response = await apiClient.patch<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.FINALIZE(attemptId),
            { feedback }
        );
        return response.data;
    },

    // ---------- Student ----------

    getAvailable: async (courseId: string): Promise<Exam[]> => {
        const response = await apiClient.get<ApiResponse<Exam[]>>(
            ENDPOINTS.EXAMS.AVAILABLE(courseId)
        );
        return response.data;
    },

    start: async (examId: string): Promise<ExamAttempt> => {
        const response = await apiClient.post<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.START(examId)
        );
        return response.data;
    },

    getMyAttempt: async (examId: string): Promise<ExamAttempt> => {
        const response = await apiClient.get<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.MY_ATTEMPT(examId)
        );
        return response.data;
    },

    saveAnswer: async (examId: string, data: ExamAnswerRequest): Promise<ExamAttempt> => {
        const response = await apiClient.put<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.SAVE_ANSWER(examId), data
        );
        return response.data;
    },

    submit: async (examId: string): Promise<ExamAttempt> => {
        const response = await apiClient.post<ApiResponse<ExamAttempt>>(
            ENDPOINTS.EXAMS.SUBMIT(examId)
        );
        return response.data;
    },

    getMyAttempts: async (): Promise<ExamAttempt[]> => {
        const response = await apiClient.get<ApiResponse<ExamAttempt[]>>(
            ENDPOINTS.EXAMS.MY_ATTEMPTS
        );
        return response.data;
    },
};