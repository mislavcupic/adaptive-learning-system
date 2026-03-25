import { apiClient, uploadFile} from '../api'
import type { TeacherNote, AnalyticNote } from '../types';

export const noteService = {
    getByStudent: async (studentId: string): Promise<TeacherNote[]> => {
        return await apiClient.get<TeacherNote[]>(`/notes/student/${studentId}`);
    },

    create: async (data: { studentId: string; note: string }): Promise<TeacherNote> => {
        return await apiClient.post<TeacherNote>('/notes', data);
    },

    update: async (id: string, note: string): Promise<TeacherNote> => {
        return await apiClient.put<TeacherNote>(`/notes/${id}`, { note });
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(`/notes/${id}`);
    },

    importFromCsv: async (studentId: string, file: File): Promise<TeacherNote[]> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('studentId', studentId);
        return await uploadFile<TeacherNote[]>('/notes/import', formData);
    },

    getAnalyticNotes: async (studentId: string): Promise<AnalyticNote[]> => {
        return await apiClient.get<AnalyticNote[]>(`/notes/analytic/${studentId}`);
    },
};