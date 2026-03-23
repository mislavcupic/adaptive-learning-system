// CLASS SERVICE - Razredi, random grupe


import { api, ENDPOINTS } from '../api';
import type { SchoolClass, Student, ClassStatistics } from '../types';

export const classService = {
    /**
     * Dohvati sve razrede
     */
    getAll: async (): Promise<SchoolClass[]> => {
        const response = await api.get<SchoolClass[]>(ENDPOINTS.CLASSES.BASE);
        return response.data;
    },

    /**
     * Dohvati razred po ID-u
     */
    getById: async (id: string): Promise<SchoolClass> => {
        const response = await api.get<SchoolClass>(ENDPOINTS.CLASSES.BY_ID(id));
        return response.data;
    },

    /**
     * Kreiraj novi razred
     */
    create: async (data: {
        name: string;
        schoolYear: string;
    }): Promise<SchoolClass> => {
        const response = await api.post<SchoolClass>(ENDPOINTS.CLASSES.BASE, data);
        return response.data;
    },

    /**
     * Ažuriraj razred
     */
    update: async (id: string, data: Partial<SchoolClass>): Promise<SchoolClass> => {
        const response = await api.put<SchoolClass>(ENDPOINTS.CLASSES.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši razred
     */
    delete: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.CLASSES.BY_ID(id));
    },

    /**
     * Dohvati studente razreda
     */
    getStudents: async (classId: string): Promise<Student[]> => {
        const response = await api.get<Student[]>(ENDPOINTS.CLASSES.STUDENTS(classId));
        return response.data;
    },

    /**
     * RANDOM rasporedi studente u EXPERIMENTAL/CONTROL grupe
     */
    randomizeGroups: async (classId: string): Promise<{
        experimentalCount: number;
        controlCount: number;
    }> => {
        const response = await api.post<{
            experimentalCount: number;
            controlCount: number;
        }>(ENDPOINTS.CLASSES.RANDOMIZE_GROUPS(classId));
        return response.data;
    },

    /**
     * Dohvati statistike razreda
     */
    getStatistics: async (classId: string): Promise<ClassStatistics> => {
        const response = await api.get<ClassStatistics>(
            ENDPOINTS.CLASSES.STATISTICS(classId)
        );
        return response.data;
    },
};