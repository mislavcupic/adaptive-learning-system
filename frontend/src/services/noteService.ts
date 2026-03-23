// ============================================
// NOTE SERVICE - Bilješke (AI, teacher, CSV)
// ============================================

import { api, ENDPOINTS } from '../api';
import type { AnalyticNote, TeacherNote, CsvImportResult } from '../types';

export const noteService = {
    // ============ TEACHER NOTES ============

    /**
     *
     */
    getTeacherNotes: async (studentId: string): Promise<TeacherNote[]> => {
        const response = await api.get<TeacherNote[]>(
            ENDPOINTS.NOTES.TEACHER_BY_STUDENT(studentId)
        );
        return response.data;
    },

    /**
     *
     */
    createTeacherNote: async (data: {
        studentId: string;
        note: string;
    }): Promise<TeacherNote> => {
        const response = await api.post<TeacherNote>(ENDPOINTS.NOTES.TEACHER, data);
        return response.data;
    },

    /**
     *
     */
    updateTeacherNote: async (
        noteId: string,
        note: string
    ): Promise<TeacherNote> => {
        const response = await api.put<TeacherNote>(
            `${ENDPOINTS.NOTES.TEACHER}/${noteId}`,
            { note }
        );
        return response.data;
    },

    /**
     * Obriši bilješku nastavnika
     */
    deleteTeacherNote: async (noteId: string): Promise<void> => {
        await api.delete(`${ENDPOINTS.NOTES.TEACHER}/${noteId}`);
    },

    /**
     * Import bilješki iz CSV-a (e-Dnevnik export)
     */
    importNotesFromCsv: async (
        classId: string,
        file: File
    ): Promise<CsvImportResult> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('classId', classId);

        const response = await api.upload<CsvImportResult>(
            ENDPOINTS.NOTES.IMPORT_CSV,
            formData
        );
        return response.data;
    },

    /**
     * get AI generirane bilješke za studenta
     */
    getAnalyticNotes: async (
        studentId: string,
        params?: {
            skillTag?: string;
            limit?: number;
        }
    ): Promise<AnalyticNote[]> => {
        const queryParams = new URLSearchParams();
        if (params?.skillTag) queryParams.append('skillTag', params.skillTag);
        if (params?.limit) queryParams.append('limit', params.limit.toString());

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.NOTES.ANALYTIC(studentId)}?${queryParams}`
            : ENDPOINTS.NOTES.ANALYTIC(studentId);

        const response = await api.get<AnalyticNote[]>(endpoint);
        return response.data;
    },
};