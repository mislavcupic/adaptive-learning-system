import { apiClient, ENDPOINTS } from '../api';
import type { Submission, PaginatedResponse } from '../types';

export const submissionService = {
    submit: async (taskId: string, code: string): Promise<Submission> => {
        return await apiClient.post<Submission>(ENDPOINTS.SUBMISSIONS.BASE, { taskId, code });
    },

    getById: async (id: string): Promise<Submission> => {
        return await apiClient.get<Submission>(ENDPOINTS.SUBMISSIONS.BY_ID(id));
    },

    getByTask: async (taskId: string, params?: {
        page?: number;
        pageSize?: number;
    }): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.page) queryParams.append('page', params.page.toString());
        if (params?.pageSize) queryParams.append('pageSize', params.pageSize.toString());

        const url = `${ENDPOINTS.SUBMISSIONS.BY_TASK(taskId)}?${queryParams}`;
        return await apiClient.get<PaginatedResponse<Submission>>(url);
    },

    getByStudent: async (studentId: string, params?: {
        page?: number;
        pageSize?: number;
    }): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.page) queryParams.append('page', params.page.toString());
        if (params?.pageSize) queryParams.append('pageSize', params.pageSize.toString());

        const url = `${ENDPOINTS.SUBMISSIONS.BASE}/student/${studentId}?${queryParams}`;
        return await apiClient.get<PaginatedResponse<Submission>>(url);
    },

    addTeacherFeedback: async (id: string, feedback: string, score?: number): Promise<Submission> => {
        return await apiClient.patch<Submission>(ENDPOINTS.SUBMISSIONS.BY_ID(id), {
            teacherFeedback: feedback,
            teacherScore: score,
        });
    },
};