import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, Submission, SubmissionRequest, PaginatedResponse } from '../types';

export const submissionService = {
    getAll: async (): Promise<Submission[]> => {
        const response = await apiClient.get<ApiResponse<Submission[]>>(ENDPOINTS.SUBMISSIONS.BASE);
        return response.data;
    },

    getPaginated: async (page = 0, size = 10): Promise<PaginatedResponse<Submission>> => {
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Submission>>>(
            ENDPOINTS.SUBMISSIONS.BASE,
            { params: { page: page.toString(), size: size.toString() } }
        );
        return response.data;
    },

    getById: async (id: string): Promise<Submission> => {
        const response = await apiClient.get<ApiResponse<Submission>>(
            ENDPOINTS.SUBMISSIONS.BY_ID(id)
        );
        return response.data;
    },

    getMy: async (): Promise<Submission[]> => {
        const response = await apiClient.get<ApiResponse<Submission[]>>(ENDPOINTS.SUBMISSIONS.MY);
        return response.data;
    },

    getByTask: async (taskId: string): Promise<Submission[]> => {
        const response = await apiClient.get<ApiResponse<Submission[]>>(
            ENDPOINTS.SUBMISSIONS.BY_TASK(taskId)
        );
        return response.data;
    },

    getByStudent: async (studentId: string): Promise<Submission[]> => {
        const response = await apiClient.get<ApiResponse<Submission[]>>(
            ENDPOINTS.SUBMISSIONS.BY_STUDENT(studentId)
        );
        return response.data;
    },

    submit: async (data: SubmissionRequest): Promise<Submission> => {
        const response = await apiClient.post<ApiResponse<Submission>>(
            ENDPOINTS.SUBMISSIONS.SUBMIT, 
            data
        );
        return response.data;
    },

    addTeacherFeedback: async (id: string, feedback: string): Promise<Submission> => {
        const response = await apiClient.patch<ApiResponse<Submission>>(
            ENDPOINTS.SUBMISSIONS.FEEDBACK(id),
            { teacherFeedback: feedback }
        );
        return response.data;
    },
};
