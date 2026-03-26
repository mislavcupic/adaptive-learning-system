import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, SchoolClass, SchoolClassRequest, User, PaginatedResponse } from '../types';

export const classService = {
    getAll: async (): Promise<SchoolClass[]> => {
        const response = await apiClient.get<ApiResponse<SchoolClass[]>>(ENDPOINTS.CLASSES.BASE);
        return response.data;
    },

    getPaginated: async (page = 0, size = 10): Promise<PaginatedResponse<SchoolClass>> => {
        const response = await apiClient.get<ApiResponse<PaginatedResponse<SchoolClass>>>(
            ENDPOINTS.CLASSES.BASE,
            { params: { page: page.toString(), size: size.toString() } }
        );
        return response.data;
    },

    getById: async (id: string): Promise<SchoolClass> => {
        const response = await apiClient.get<ApiResponse<SchoolClass>>(ENDPOINTS.CLASSES.BY_ID(id));
        return response.data;
    },

    create: async (data: SchoolClassRequest): Promise<SchoolClass> => {
        const response = await apiClient.post<ApiResponse<SchoolClass>>(ENDPOINTS.CLASSES.BASE, data);
        return response.data;
    },

    update: async (id: string, data: SchoolClassRequest): Promise<SchoolClass> => {
        const response = await apiClient.put<ApiResponse<SchoolClass>>(
            ENDPOINTS.CLASSES.BY_ID(id), 
            data
        );
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.CLASSES.BY_ID(id));
    },

    getStudents: async (classId: string): Promise<User[]> => {
        const response = await apiClient.get<ApiResponse<User[]>>(
            ENDPOINTS.CLASSES.STUDENTS(classId)
        );
        return response.data;
    },

    addStudent: async (classId: string, studentId: string): Promise<void> => {
        await apiClient.post<ApiResponse<void>>(
            ENDPOINTS.CLASSES.ADD_STUDENT(classId, studentId)
        );
    },

    removeStudent: async (classId: string, studentId: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(
            ENDPOINTS.CLASSES.ADD_STUDENT(classId, studentId)
        );
    },
};
