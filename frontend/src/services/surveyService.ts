import { apiClient } from '../api';
import type { Survey, SurveyResponse } from '../types';

export const surveyService = {
    getAll: async (): Promise<Survey[]> => {
        return await apiClient.get<Survey[]>('/surveys');
    },

    getActive: async (): Promise<Survey[]> => {
        return await apiClient.get<Survey[]>('/surveys/active');
    },

    getById: async (id: string): Promise<Survey> => {
        return await apiClient.get<Survey>(`/surveys/${id}`);
    },

    create: async (data: Partial<Survey>): Promise<Survey> => {
        return await apiClient.post<Survey>('/surveys', data);
    },

    update: async (id: string, data: Partial<Survey>): Promise<Survey> => {
        return await apiClient.put<Survey>(`/surveys/${id}`, data);
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(`/surveys/${id}`);
    },

    activate: async (id: string): Promise<Survey> => {
        return await apiClient.post<Survey>(`/surveys/${id}/activate`);
    },

    submitResponse: async (surveyId: string, answers: Record<string, unknown>): Promise<SurveyResponse> => {
        return await apiClient.post<SurveyResponse>(`/surveys/${surveyId}/responses`, { answers });
    },

    getResponses: async (surveyId: string): Promise<SurveyResponse[]> => {
        return await apiClient.get<SurveyResponse[]>(`/surveys/${surveyId}/responses`);
    },

    getStatistics: async (surveyId: string): Promise<{
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
        return await apiClient.get(`/surveys/${surveyId}/statistics`);
    },
};