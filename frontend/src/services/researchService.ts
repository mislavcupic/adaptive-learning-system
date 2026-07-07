import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, ResearchResult, AncovaResponse } from '../types';

export const researchService = {
    getResults: async (): Promise<ResearchResult[]> => {
        const response = await apiClient.get<ApiResponse<ResearchResult[]>>(
            ENDPOINTS.RESEARCH.RESULTS
        );
        return response.data;
    },

    getAncova: async (): Promise<AncovaResponse> => {
        const response = await apiClient.get<ApiResponse<AncovaResponse>>(
            ENDPOINTS.RESEARCH.ANCOVA
        );
        return response.data;
    },
}