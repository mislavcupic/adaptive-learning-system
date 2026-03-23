// SUBMISSION SERVICE - Predaje koda, feedback


import { api, ENDPOINTS } from '../api';
import type { Submission, PaginatedResponse } from '../types';

export const submissionService = {
    /**
     *
     */
    submit: async (data: {
        taskId: string;
        code: string;
    }): Promise<Submission> => {
        const response = await api.post<Submission>(ENDPOINTS.SUBMISSIONS.SUBMIT, data);
        return response.data;
    },

    /**
     * get predaju po ID-u
     */
    getById: async (id: string): Promise<Submission> => {
        const response = await api.get<Submission>(ENDPOINTS.SUBMISSIONS.BY_ID(id));
        return response.data;
    },

    /**
     * get predaje
     */
    getByStudent: async (
        studentId: string,
        params?: {
            taskId?: string;
            page?: number;
            pageSize?: number;
        }
    ): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.taskId) queryParams.append('taskId', params.taskId);
        if (params?.page) queryParams.append('page', params.page.toString());
        if (params?.pageSize) queryParams.append('pageSize', params.pageSize.toString());

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.SUBMISSIONS.BY_STUDENT(studentId)}?${queryParams}`
            : ENDPOINTS.SUBMISSIONS.BY_STUDENT(studentId);

        const response = await api.get<PaginatedResponse<Submission>>(endpoint);
        return response.data;
    },

    /**
     * get predaje za zadatak (Teacher view)
     */
    getByTask: async (
        taskId: string,
        params?: {
            classId?: string;
            groupType?: 'EXPERIMENTAL' | 'CONTROL';
            page?: number;
            pageSize?: number;
        }
    ): Promise<PaginatedResponse<Submission>> => {
        const queryParams = new URLSearchParams();
        if (params?.classId) queryParams.append('classId', params.classId);
        if (params?.groupType) queryParams.append('groupType', params.groupType);
        if (params?.page) queryParams.append('page', params.page.toString());
        if (params?.pageSize) queryParams.append('pageSize', params.pageSize.toString());

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.SUBMISSIONS.BY_TASK(taskId)}?${queryParams}`
            : ENDPOINTS.SUBMISSIONS.BY_TASK(taskId);

        const response = await api.get<PaginatedResponse<Submission>>(endpoint);
        return response.data;
    },

    /**
     * Teacher dodaje ručni feedback (za CONTROL grupu)
     */
    addTeacherFeedback: async (
        submissionId: string,
        data: {
            feedback: string;
            score: number;
        }
    ): Promise<Submission> => {
        const response = await api.patch<Submission>(
            ENDPOINTS.SUBMISSIONS.BY_ID(submissionId),
            {
                teacherFeedback: data.feedback,
                teacherScore: data.score,
            }
        );
        return response.data;
    },
};