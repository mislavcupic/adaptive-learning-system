import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, Task, TaskRequest, PaginatedResponse } from '../types';

export const taskService = {
    getAll: async (): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>(ENDPOINTS.TASKS.BASE);
        return response.data;
    },

    getPaginated: async (page = 0, size = 10): Promise<PaginatedResponse<Task>> => {
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Task>>>(
            ENDPOINTS.TASKS.BASE,
            { params: { page: page.toString(), size: size.toString() } }
        );
        return response.data;
    },

    getById: async (id: string): Promise<Task> => {
        const response = await apiClient.get<ApiResponse<Task>>(ENDPOINTS.TASKS.BY_ID(id));
        return response.data;
    },

    getByCourse: async (courseId: string): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>(
            ENDPOINTS.TASKS.BY_COURSE(courseId)
        );
        return response.data;
    },

    create: async (data: TaskRequest): Promise<Task> => {
        const response = await apiClient.post<ApiResponse<Task>>(ENDPOINTS.TASKS.BASE, data);
        return response.data;
    },

    update: async (id: string, data: TaskRequest): Promise<Task> => {
        const response = await apiClient.put<ApiResponse<Task>>(ENDPOINTS.TASKS.BY_ID(id), data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.TASKS.BY_ID(id));
    },
};
