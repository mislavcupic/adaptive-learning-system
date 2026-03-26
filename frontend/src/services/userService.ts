import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, User, PaginatedResponse } from '../types';

export const userService = {
    getAll: async (): Promise<User[]> => {
        const response = await apiClient.get<ApiResponse<User[]>>(ENDPOINTS.USERS.BASE);
        return response.data;
    },

    getPaginated: async (page = 0, size = 10): Promise<PaginatedResponse<User>> => {
        const response = await apiClient.get<ApiResponse<PaginatedResponse<User>>>(
            ENDPOINTS.USERS.BASE,
            { params: { page: page.toString(), size: size.toString() } }
        );
        return response.data;
    },

    getById: async (id: string): Promise<User> => {
        const response = await apiClient.get<ApiResponse<User>>(ENDPOINTS.USERS.BY_ID(id));
        return response.data;
    },

    getStudents: async (): Promise<User[]> => {
        const response = await apiClient.get<ApiResponse<User[]>>(ENDPOINTS.USERS.STUDENTS);
        return response.data;
    },

    getTeachers: async (): Promise<User[]> => {
        const response = await apiClient.get<ApiResponse<User[]>>(ENDPOINTS.USERS.TEACHERS);
        return response.data;
    },

    update: async (id: string, data: Partial<User>): Promise<User> => {
        const response = await apiClient.put<ApiResponse<User>>(ENDPOINTS.USERS.BY_ID(id), data);
        return response.data;
    },

    activate: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.USERS.ACTIVATE(id));
    },

    deactivate: async (id: string): Promise<void> => {
        await apiClient.patch<ApiResponse<void>>(ENDPOINTS.USERS.DEACTIVATE(id));
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.USERS.BY_ID(id));
    },
};
