import { apiClient } from '../api';
import type { Survey, SurveyResponse } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

interface SurveyStatistics {
    totalResponses: number;
    responsesByRole: Record<string, number>;
    responsesByGroup: Record<string, number>;
    questionStats: Array<{
        questionId: string;
        questionText: string;
        averageScore?: number;
        distribution: Record<string, number>;
    }>;
}

export const surveyService = {
    getAll: async (): Promise<Survey[]> => {
        const response = await apiClient.get<ApiResponse<Survey[]>>('/surveys');
        return response.data;
    },

    getActive: async (): Promise<Survey[]> => {
        const response = await apiClient.get<ApiResponse<Survey[]>>('/surveys/active');
        return response.data;
    },

    getById: async (id: string): Promise<Survey> => {
        const response = await apiClient.get<ApiResponse<Survey>>(`/surveys/${id}`);
        return response.data;
    },

    create: async (data: Partial<Survey>): Promise<Survey> => {
        const response = await apiClient.post<ApiResponse<Survey>>('/surveys', data);
        return response.data;
    },

    update: async (id: string, data: Partial<Survey>): Promise<Survey> => {
        const response = await apiClient.put<ApiResponse<Survey>>(`/surveys/${id}`, data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(`/surveys/${id}`);
    },

    activate: async (id: string): Promise<Survey> => {
        const response = await apiClient.post<ApiResponse<Survey>>(`/surveys/${id}/activate`);
        return response.data;
    },

    submitResponse: async (surveyId: string, answers: Record<string, unknown>): Promise<SurveyResponse> => {
        const response = await apiClient.post<ApiResponse<SurveyResponse>>(
            `/surveys/${surveyId}/responses`, 
            { answers }
        );
        return response.data;
    },

    getResponses: async (surveyId: string): Promise<SurveyResponse[]> => {
        const response = await apiClient.get<ApiResponse<SurveyResponse[]>>(
            `/surveys/${surveyId}/responses`
        );
        return response.data;
    },

    getStatistics: async (surveyId: string): Promise<SurveyStatistics> => {
        const response = await apiClient.get<ApiResponse<SurveyStatistics>>(
            `/surveys/${surveyId}/statistics`
        );
        return response.data;
    },
};
