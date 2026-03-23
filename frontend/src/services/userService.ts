// ============================================
// USER SERVICE - CRUD korisnika, CSV import
// ============================================

import { api, ENDPOINTS } from '../api';
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
        const response = await api.get<PaginatedResponse<Student>>(endpoint);
        return response.data;
    },

    /**
     * Dohvati studenta po ID-u
     */
    getStudentById: async (id: string): Promise<Student> => {
        const response = await api.get<Student>(ENDPOINTS.USERS.BY_ID(id));
        return response.data;
    },

    /**
     * Dohvati sve nastavnike
     */
    getTeachers: async (): Promise<Teacher[]> => {
        const response = await api.get<Teacher[]>(ENDPOINTS.USERS.TEACHERS);
        return response.data;
    },

    /**
     * Ažuriraj korisnika
     */
    updateUser: async (id: string, data: Partial<User>): Promise<User> => {
        const response = await api.put<User>(ENDPOINTS.USERS.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši korisnika
     */
    deleteUser: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.USERS.BY_ID(id));
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

        const response = await api.upload<CsvImportResult>(
            ENDPOINTS.USERS.IMPORT_CSV,
            formData
        );
        return response.data;
    },
};