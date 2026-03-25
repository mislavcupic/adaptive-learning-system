import { apiClient, uploadFile } from '../api';
import type { TeacherNote, AnalyticNote } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const noteService = {
    getByStudent: async (studentId: string): Promise<TeacherNote[]> => {
        const response = await apiClient.get<ApiResponse<TeacherNote[]>>(
            `/notes/student/${studentId}`
        );
        return response.data;
    },

    create: async (data: { studentId: string; note: string }): Promise<TeacherNote> => {
        const response = await apiClient.post<ApiResponse<TeacherNote>>('/notes', data);
        return response.data;
    },

    update: async (id: string, note: string): Promise<TeacherNote> => {
        const response = await apiClient.put<ApiResponse<TeacherNote>>(`/notes/${id}`, { note });
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(`/notes/${id}`);
    },

    importFromCsv: async (studentId: string, file: File): Promise<TeacherNote[]> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('studentId', studentId);
        const response = await uploadFile<ApiResponse<TeacherNote[]>>('/notes/import', formData);
        return response.data;
    },

    getAnalyticNotes: async (studentId: string): Promise<AnalyticNote[]> => {
        const response = await apiClient.get<ApiResponse<AnalyticNote[]>>(
            `/notes/analytic/${studentId}`
        );
        return response.data;
    },
};
