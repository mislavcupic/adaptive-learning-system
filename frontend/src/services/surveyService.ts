// SURVEY SERVICE - Upitnici za evaluaciju sustava


import { api, ENDPOINTS } from '../api';
import type {
    Survey,
    SurveyQuestion,
    SurveyResponse,
    SurveyAnswer,
    SurveyTargetRole,
} from '../types';

export const surveyService = {
    /**
     *
     */
    getAll: async (): Promise<Survey[]> => {
        const response = await api.get<Survey[]>(ENDPOINTS.SURVEYS.BASE);
        return response.data;
    },

    /**
     * Dohvati aktivne upitnike za trenutnog korisnika
     */
    getActive: async (): Promise<Survey[]> => {
        const response = await api.get<Survey[]>(ENDPOINTS.SURVEYS.ACTIVE);
        return response.data;
    },

    /**
     * Dohvati upitnik po ID-u
     */
    getById: async (id: string): Promise<Survey> => {
        const response = await api.get<Survey>(ENDPOINTS.SURVEYS.BY_ID(id));
        return response.data;
    },

    /**
     * Kreiraj novi upitnik (Admin only)
     */
    create: async (data: {
        title: string;
        description: string;
        targetRole: SurveyTargetRole;
        questions: Omit<SurveyQuestion, 'id' | 'surveyId'>[];
        availableFrom: string;
        availableTo: string;
    }): Promise<Survey> => {
        const response = await api.post<Survey>(ENDPOINTS.SURVEYS.BASE, data);
        return response.data;
    },

    /**
     * Ažuriraj upitnik
     */
    update: async (id: string, data: Partial<Survey>): Promise<Survey> => {
        const response = await api.put<Survey>(ENDPOINTS.SURVEYS.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši upitnik
     */
    delete: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.SURVEYS.BY_ID(id));
    },

    /**
     * Aktiviraj/deaktiviraj upitnik
     */
    toggleActive: async (id: string, isActive: boolean): Promise<Survey> => {
        const response = await api.patch<Survey>(ENDPOINTS.SURVEYS.BY_ID(id), {
            isActive,
        });
        return response.data;
    },

    /**
     * Pošalji odgovore na upitnik
     */
    submitResponse: async (
        surveyId: string,
        answers: SurveyAnswer[]
    ): Promise<SurveyResponse> => {
        const response = await api.post<SurveyResponse>(
            ENDPOINTS.SURVEYS.RESPOND(surveyId),
            { answers }
        );
        return response.data;
    },

    /**
     * Dohvati sve odgovore na upitnik (Admin only)
     */
    getResponses: async (surveyId: string): Promise<SurveyResponse[]> => {
        const response = await api.get<SurveyResponse[]>(
            ENDPOINTS.SURVEYS.RESPONSES(surveyId)
        );
        return response.data;
    },

    /**
     * Dohvati analitiku odgovora (Admin only)
     */
    getAnalytics: async (surveyId: string): Promise<{
        totalResponses: number;
        responsesByRole: Record<string, number>;
        responsesByGroup: Record<string, number>;
        questionStats: Array<{
            questionId: string;
            questionText: string;
            averageScore?: number;
            distribution: Record<string, number>;
        }>;
    }> => {
        const response = await api.get<{
            totalResponses: number;
            responsesByRole: Record<string, number>;
            responsesByGroup: Record<string, number>;
            questionStats: Array<{
                questionId: string;
                questionText: string;
                averageScore?: number;
                distribution: Record<string, number>;
            }>;
        }>(ENDPOINTS.SURVEYS.ANALYTICS(surveyId));
        return response.data;
    },
};