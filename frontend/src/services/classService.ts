import { apiClient, ENDPOINTS } from '../api';
import type { SchoolClass, Student, ClassStatistics } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const classService = {
    getAll: async (): Promise<SchoolClass[]> => {
        const response = await apiClient.get<ApiResponse<SchoolClass[]>>(ENDPOINTS.CLASSES.BASE);
        return response.data;
    },

    getById: async (id: string): Promise<SchoolClass> => {
        const response = await apiClient.get<ApiResponse<SchoolClass>>(ENDPOINTS.CLASSES.BY_ID(id));
        return response.data;
    },

    create: async (data: Partial<SchoolClass>): Promise<SchoolClass> => {
        const response = await apiClient.post<ApiResponse<SchoolClass>>(ENDPOINTS.CLASSES.BASE, data);
        return response.data;
    },

    update: async (id: string, data: Partial<SchoolClass>): Promise<SchoolClass> => {
        const response = await apiClient.put<ApiResponse<SchoolClass>>(ENDPOINTS.CLASSES.BY_ID(id), data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.CLASSES.BY_ID(id));
    },

    getStudents: async (classId: string): Promise<Student[]> => {
        const response = await apiClient.get<ApiResponse<Student[]>>(
            `${ENDPOINTS.CLASSES.BY_ID(classId)}/students`
        );
        return response.data;
    },

    addStudent: async (classId: string, studentId: string): Promise<void> => {
        await apiClient.post(`${ENDPOINTS.CLASSES.BY_ID(classId)}/students/${studentId}`);
    },

    removeStudent: async (classId: string, studentId: string): Promise<void> => {
        await apiClient.delete(`${ENDPOINTS.CLASSES.BY_ID(classId)}/students/${studentId}`);
    },

    getGroupCounts: async (classId: string): Promise<{ experimentalCount: number; controlCount: number }> => {
        const response = await apiClient.get<ApiResponse<{ experimentalCount: number; controlCount: number }>>(
            `${ENDPOINTS.CLASSES.BY_ID(classId)}/group-counts`
        );
        return response.data;
    },

    getStatistics: async (classId: string): Promise<ClassStatistics> => {
        const response = await apiClient.get<ApiResponse<ClassStatistics>>(
            `${ENDPOINTS.CLASSES.BY_ID(classId)}/statistics`
        );
        return response.data;
    },
};
