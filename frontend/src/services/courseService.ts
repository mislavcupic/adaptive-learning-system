import { apiClient, ENDPOINTS } from '../api';
import type { Course, LearningOutcome } from '../types';

export const courseService = {
    getAll: async (): Promise<Course[]> => {
        return await apiClient.get<Course[]>(ENDPOINTS.COURSES.BASE);
    },

    getById: async (id: string): Promise<Course> => {
        return await apiClient.get<Course>(ENDPOINTS.COURSES.BY_ID(id));
    },

    create: async (data: Partial<Course>): Promise<Course> => {
        return await apiClient.post<Course>(ENDPOINTS.COURSES.BASE, data);
    },

    update: async (id: string, data: Partial<Course>): Promise<Course> => {
        return await apiClient.put<Course>(ENDPOINTS.COURSES.BY_ID(id), data);
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.COURSES.BY_ID(id));
    },

    getOutcomes: async (courseId: string): Promise<LearningOutcome[]> => {
        return await apiClient.get<LearningOutcome[]>(`${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes`);
    },

    createOutcome: async (courseId: string, data: Partial<LearningOutcome>): Promise<LearningOutcome> => {
        return await apiClient.post<LearningOutcome>(`${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes`, data);
    },

    updateOutcome: async (courseId: string, outcomeId: string, data: Partial<LearningOutcome>): Promise<LearningOutcome> => {
        return await apiClient.put<LearningOutcome>(
            `${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes/${outcomeId}`,
            data
        );
    },

    deleteOutcome: async (courseId: string, outcomeId: string): Promise<void> => {
        await apiClient.delete(`${ENDPOINTS.COURSES.BY_ID(courseId)}/outcomes/${outcomeId}`);
    },
};