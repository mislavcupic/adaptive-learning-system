import { apiClient, uploadFile, ENDPOINTS } from '../api';
import type { User, Student, Teacher, CsvImportResult, PaginatedResponse } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const userService = {
    getStudents: async (params?: {
        classId?: string;
        groupType?: 'EXPERIMENTAL' | 'CONTROL';
        page?: number;
        size?: number;
    }): Promise<PaginatedResponse<Student>> => {
        const queryParams = new URLSearchParams();
        if (params?.classId) queryParams.append('classId', params.classId);
        if (params?.groupType) queryParams.append('groupType', params.groupType);
        if (params?.page !== undefined) queryParams.append('page', params.page.toString());
        if (params?.size) queryParams.append('size', params.size.toString());

        const endpoint = `${ENDPOINTS.USERS.STUDENTS}?${queryParams}`;
        const response = await apiClient.get<ApiResponse<PaginatedResponse<Student>>>(endpoint);
        return response.data;
    },

    getAllStudents: async (): Promise<Student[]> => {
        const response = await apiClient.get<ApiResponse<Student[]>>(ENDPOINTS.USERS.STUDENTS);
        return response.data;
    },

    getStudentById: async (id: string): Promise<Student> => {
        const response = await apiClient.get<ApiResponse<Student>>(ENDPOINTS.USERS.BY_ID(id));
        return response.data;
    },

    getTeachers: async (): Promise<Teacher[]> => {
        const response = await apiClient.get<ApiResponse<Teacher[]>>(ENDPOINTS.USERS.TEACHERS);
        return response.data;
    },

    getById: async (id: string): Promise<User> => {
        const response = await apiClient.get<ApiResponse<User>>(ENDPOINTS.USERS.BY_ID(id));
        return response.data;
    },

    updateUser: async (id: string, data: Partial<User>): Promise<User> => {
        const response = await apiClient.patch<ApiResponse<User>>(ENDPOINTS.USERS.BY_ID(id), data);
        return response.data;
    },

    updateRole: async (id: string, role: string): Promise<User> => {
        const response = await apiClient.patch<ApiResponse<User>>(
            `${ENDPOINTS.USERS.BY_ID(id)}/role`, 
            { role }
        );
        return response.data;
    },

    updateGroup: async (id: string, groupType: string): Promise<User> => {
        const response = await apiClient.patch<ApiResponse<User>>(
            `${ENDPOINTS.USERS.BY_ID(id)}/group`, 
            { groupType }
        );
        return response.data;
    },

    activate: async (id: string): Promise<void> => {
        await apiClient.patch(`${ENDPOINTS.USERS.BY_ID(id)}/activate`);
    },

    deactivate: async (id: string): Promise<void> => {
        await apiClient.patch(`${ENDPOINTS.USERS.BY_ID(id)}/deactivate`);
    },

    deleteUser: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.USERS.BY_ID(id));
    },

    importStudentsFromCsv: async (classId: string, file: File): Promise<CsvImportResult> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('classId', classId);
        
        const response = await uploadFile<ApiResponse<CsvImportResult>>(
            ENDPOINTS.USERS.IMPORT_CSV, 
            formData
        );
        return response.data;
    },
};
