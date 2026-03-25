import { apiClient, ENDPOINTS } from '../api';
import type { SchoolClass, Student, ClassStatistics } from '../types';

export const classService = {
    getAll: async (): Promise<SchoolClass[]> => {
        return await apiClient.get<SchoolClass[]>(ENDPOINTS.CLASSES.BASE);
    },

    getById: async (id: string): Promise<SchoolClass> => {
        return await apiClient.get<SchoolClass>(ENDPOINTS.CLASSES.BY_ID(id));
    },

    create: async (data: Partial<SchoolClass>): Promise<SchoolClass> => {
        return await apiClient.post<SchoolClass>(ENDPOINTS.CLASSES.BASE, data);
    },

    update: async (id: string, data: Partial<SchoolClass>): Promise<SchoolClass> => {
        return await apiClient.put<SchoolClass>(ENDPOINTS.CLASSES.BY_ID(id), data);
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.CLASSES.BY_ID(id));
    },

    getStudents: async (classId: string): Promise<Student[]> => {
        return await apiClient.get<Student[]>(`${ENDPOINTS.CLASSES.BY_ID(classId)}/students`);
    },

    getGroupCounts: async (classId: string): Promise<{ experimentalCount: number; controlCount: number }> => {
        return await apiClient.get<{ experimentalCount: number; controlCount: number }>(
            `${ENDPOINTS.CLASSES.BY_ID(classId)}/group-counts`
        );
    },

    getStatistics: async (classId: string): Promise<ClassStatistics> => {
        return await apiClient.get<ClassStatistics>(`${ENDPOINTS.CLASSES.BY_ID(classId)}/statistics`);
    },
};