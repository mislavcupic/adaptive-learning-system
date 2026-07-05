import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, ResearchResult } from '../types';

export const researchService = {
    getResults: async (): Promise<ResearchResult[]> => {
        const response = await apiClient.get<ApiResponse<ResearchResult[]>>(
            ENDPOINTS.RESEARCH.RESULTS
        );
        return response.data;
    },
}