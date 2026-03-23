// COURSE SERVICE - Kolegiji, ishodi, kriteriji


import { api, ENDPOINTS } from '../api';
import type { Course, LearningOutcome, LanguageType } from '../types';

export const courseService = {
    /**
     * Dohvati sve kolegije
     */
    getAll: async (): Promise<Course[]> => {
        const response = await api.get<Course[]>(ENDPOINTS.COURSES.BASE);
        return response.data;
    },

    /**
     * Dohvati kolegij po ID-u
     */
    getById: async (id: string): Promise<Course> => {
        const response = await api.get<Course>(ENDPOINTS.COURSES.BY_ID(id));
        return response.data;
    },

    /**
     * Kreiraj novi kolegij
     */
    create: async (data: {
        name: string;
        code: string;
        description: string;
        languageType: LanguageType;
        classIds: string[];
    }): Promise<Course> => {
        const response = await api.post<Course>(ENDPOINTS.COURSES.BASE, data);
        return response.data;
    },

    /**
     * Ažuriraj kolegij
     */
    update: async (id: string, data: Partial<Course>): Promise<Course> => {
        const response = await api.put<Course>(ENDPOINTS.COURSES.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši kolegij
     */
    delete: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.COURSES.BY_ID(id));
    },

    // ============ ISHODI ============

    /**
     * Dohvati sve ishode kolegija
     */
    getOutcomes: async (courseId: string): Promise<LearningOutcome[]> => {
        const response = await api.get<LearningOutcome[]>(
            ENDPOINTS.COURSES.OUTCOMES(courseId)
        );
        return response.data;
    },

    /**
     * Dohvati ishod po ID-u
     */
    getOutcomeById: async (
        courseId: string,
        outcomeId: string
    ): Promise<LearningOutcome> => {
        const response = await api.get<LearningOutcome>(
            ENDPOINTS.COURSES.OUTCOME_BY_ID(courseId, outcomeId)
        );
        return response.data;
    },

    /**
     * Kreiraj novi ishod
     */
    createOutcome: async (
        courseId: string,
        data: {
            code: string;
            description: string;
            skills: string[];
            orderIndex: number;
        }
    ): Promise<LearningOutcome> => {
        const response = await api.post<LearningOutcome>(
            ENDPOINTS.COURSES.OUTCOMES(courseId),
            data
        );
        return response.data;
    },

    /**
     * Ažuriraj ishod
     */
    updateOutcome: async (
        courseId: string,
        outcomeId: string,
        data: Partial<LearningOutcome>
    ): Promise<LearningOutcome> => {
        const response = await api.put<LearningOutcome>(
            ENDPOINTS.COURSES.OUTCOME_BY_ID(courseId, outcomeId),
            data
        );
        return response.data;
    },

    /**
     * Obriši ishod
     */
    deleteOutcome: async (courseId: string, outcomeId: string): Promise<void> => {
        await api.delete(ENDPOINTS.COURSES.OUTCOME_BY_ID(courseId, outcomeId));
    },
};