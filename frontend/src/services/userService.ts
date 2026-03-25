// ============================================
// USER SERVICE - CRUD korisnika, CSV import
// ============================================

import { apiClient, ENDPOINTS } from '../api';
import type { User, Student, Teacher, CsvImportResult, PaginatedResponse } from '../types';

export const userService = {
    /**
     * Dohvati sve studente
     */
    getStudents: async (params?: {
        classId?: string;
        groupType?: 'EXPERIMENTAL' | 'CONTROL';
        page?: number;
        pageSize?: number;
    }): Promise<PaginatedResponse<Student>> => {
        const queryParams = new URLSearchParams();
        if (params?.classId) queryParams.append('classId', params.classId);
        if (params?.groupType) queryParams.append('groupType', params.groupType);
        if (params?.page) queryParams.append('page', params.page.toString());
        if (params?.pageSize) queryParams.append('pageSize', params.pageSize.toString());

        const endpoint = `${ENDPOINTS.USERS.STUDENTS}?${queryParams}`;
        const response = await apiClient.get<PaginatedResponse<Student>>(endpoint);
        return response;
    },

    /**
     * Dohvati studenta po ID-u
     */
    getStudentById: async (id: string): Promise<Student> => {
        const response = await apiClient.get<Student>(ENDPOINTS.USERS.BY_ID(id));
        return response;
    },

    /**
     * Dohvati sve nastavnike
     */
    getTeachers: async (): Promise<Teacher[]> => {
        const response = await apiClient.get<Teacher[]>(ENDPOINTS.USERS.TEACHERS);
        return response;
    },

    /**
     * Ažuriraj korisnika
     */
    updateUser: async (id: string, data: Partial<User>): Promise<User> => {
        const response = await apiClient.post<User>(ENDPOINTS.USERS.BY_ID(id), data);
        return response;
    },

    /**
     * Obriši korisnika
     */
    deleteUser: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.USERS.BY_ID(id));
    },

    /**
     * Import studenata iz CSV-a (e-Dnevnik export)
     */
    importStudentsFromCsv: async (
        classId: string,
        file: File
    ): Promise<CsvImportResult> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('classId', classId);

        const response = await apiClient.put<CsvImportResult>(
            ENDPOINTS.USERS.IMPORT_CSV,
            formData
        );
        return response;
    },
};