import { apiClient, ENDPOINTS } from '../api';
import type { Course, LearningOutcome } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const courseService = {
    getAll: async (): Promise<Course[]> => {
        const response = await apiClient.get<ApiResponse<Course[]>>(ENDPOINTS.COURSES.BASE);
        return response.data;
    },

    getById: async (id: string): Promise<Course> => {
        const response = await apiClient.get<ApiResponse<Course>>(ENDPOINTS.COURSES.BY_ID(id));
        return response.data;
    },

    create: async (data: Partial<Course>): Promise<Course> => {
        const response = await apiClient.post<ApiResponse<Course>>(ENDPOINTS.COURSES.BASE, data);
        return response.data;
    },

    update: async (id: string, data: Partial<Course>): Promise<Course> => {
        const response = await apiClient.put<ApiResponse<Course>>(ENDPOINTS.COURSES.BY_ID(id), data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.COURSES.BY_ID(id));
    },

    getOutcomes: async (courseId: string): Promise<LearningOutcome[]> => {
        const response = await apiClient.get<ApiResponse<LearningOutcome[]>>(
            `${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes`
        );
        return response.data;
    },

    createOutcome: async (courseId: string, data: Partial<LearningOutcome>): Promise<LearningOutcome> => {
        const response = await apiClient.post<ApiResponse<LearningOutcome>>(
            `${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes`, 
            data
        );
        return response.data;
    },

    updateOutcome: async (courseId: string, outcomeId: string, data: Partial<LearningOutcome>): Promise<LearningOutcome> => {
        const response = await apiClient.put<ApiResponse<LearningOutcome>>(
            `${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes/${outcomeId}`,
            data
        );
        return response.data;
    },

    deleteOutcome: async (courseId: string, outcomeId: string): Promise<void> => {
        await apiClient.delete(`${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes/${outcomeId}`);
    },
};
