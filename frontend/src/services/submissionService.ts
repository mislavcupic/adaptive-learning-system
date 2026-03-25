import { apiClient, ENDPOINTS } from '../api';
import type { Submission, PaginatedResponse } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const submissionService = {
    submit: async (taskId: string, code: string): Promise<Submission> => {
        const response = await apiClient.post<ApiResponse<Submission>>(
            ENDPOINTS.SUBMISSIONS.BASE, 
            { taskId, code }
        );
        return response.data;
    },

    getById: async (id: string): Promise<Submission> => {
        const response = await apiClient.get<ApiResponse<Submission>>(ENDPOINTS.SUBMISSIONS.BY_ID(id));
        return response.data;
    },

    getMy: async (): Promise<Submission[]> => {
        const response = await apiClient.get<ApiResponse<Submission[]>>('/submissions/my');
        return response.data;
    },

    getByTask: async (taskId: string, params?: {
        page?: number;
        size?: number;
    }): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.page !== undefined) queryParams.append('page', params.page.toString());
        if (params?.size) queryParams.append('size', params.size.toString());

        const url = `${ENDPOINTS.SUBMISSIONS.BY_TASK(taskId)}?${queryParams}`;
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Submission>>>(url);
        return response.data;
    },

    getByStudent: async (studentId: string, params?: {
        page?: number;
        size?: number;
    }): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.page !== undefined) queryParams.append('page', params.page.toString());
        if (params?.size) queryParams.append('size', params.size.toString());

        const url = `${ENDPOINTS.SUBMISSIONS.BY_STUDENT(studentId)}?${queryParams}`;
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Submission>>>(url);
        return response.data;
    },

    addTeacherFeedback: async (id: string, feedback: string, score?: number): Promise<Submission> => {
        const response = await apiClient.patch<ApiResponse<Submission>>(
            `${ENDPOINTS.SUBMISSIONS.BY_ID(id)}/feedback`, 
            { feedback, score }
        );
        return response.data;
    },

    getCount: async (): Promise<number> => {
        const response = await apiClient.get<ApiResponse<number>>('/submissions/my/count');
        return response.data;
    },
};
