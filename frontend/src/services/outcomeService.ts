import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, LearningOutcome, LearningOutcomeRequest, Task } from '../types';

export const outcomeService = {
    getAll: async (): Promise<LearningOutcome[]> => {
        const response = await apiClient.get<ApiResponse<LearningOutcome[]>>(ENDPOINTS.OUTCOMES.BASE);
        return response.data;
    },

    getById: async (id: string): Promise<LearningOutcome> => {
        const response = await apiClient.get<ApiResponse<LearningOutcome>>(
            ENDPOINTS.OUTCOMES.BY_ID(id)
        );
        return response.data;
    },

    create: async (data: LearningOutcomeRequest): Promise<LearningOutcome> => {
        const response = await apiClient.post<ApiResponse<LearningOutcome>>(
            ENDPOINTS.OUTCOMES.BASE, 
            data
        );
        return response.data;
    },

    update: async (id: string, data: LearningOutcomeRequest): Promise<LearningOutcome> => {
        const response = await apiClient.put<ApiResponse<LearningOutcome>>(
            ENDPOINTS.OUTCOMES.BY_ID(id), 
            data
        );
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete<ApiResponse<void>>(ENDPOINTS.OUTCOMES.BY_ID(id));
    },

    getTasks: async (outcomeId: string): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>(
            ENDPOINTS.OUTCOMES.TASKS(outcomeId)
        );
        return response.data;
    },
};
