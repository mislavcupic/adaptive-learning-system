import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, Course, CourseRequest, LearningOutcome, PaginatedResponse } from '../types';

export const courseService = {
    getAll: async (): Promise<Course[]> => {
        const response = await apiClient.get<ApiResponse<Course[]>>(ENDPOINTS.COURSES.BASE);
        return response.data;
    },

    getPaginated: async (page = 0, size = 10): Promise<PaginatedResponse<Course>> => {
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Course>>>(
            ENDPOINTS.COURSES.BASE,
            { params: { page: page.toString(), size: size.toString() } }
        );
        return response.data;
    },

    getById: async (id: string): Promise<Course> => {
        const response = await apiClient.get<ApiResponse<Course>>(ENDPOINTS.COURSES.BY_ID(id));
        return response.data;
    },

    create: async (data: CourseRequest): Promise<Course> => {
        const response = await apiClient.post<ApiResponse<Course>>(ENDPOINTS.COURSES.BASE, data);
        return response.data;
    },

    update: async (id: string, data: CourseRequest): Promise<Course> => {
        const response = await apiClient.put<ApiResponse<Course>>(ENDPOINTS.COURSES.BY_ID(id), data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.COURSES.BY_ID(id));
    },

    activate: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.COURSES.ACTIVATE(id));
    },

    deactivate: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.COURSES.DEACTIVATE(id));
    },

    getOutcomes: async (courseId: string): Promise<LearningOutcome[]> => {
        const response = await apiClient.get<ApiResponse<LearningOutcome[]>>(
            ENDPOINTS.COURSES.OUTCOMES(courseId)
        );
        return response.data;
    },
};
