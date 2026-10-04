import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, SkillMastery } from '../types';

export const masteryService = {

    getMy: async (): Promise<SkillMastery[]> => {
        const response = await apiClient.get<ApiResponse<SkillMastery[]>>(
            ENDPOINTS.MASTERY.MY
        );
        return response.data;
    },

    getMyAverage: async (): Promise<number> => {
        const response = await apiClient.get<ApiResponse<number>>(
            ENDPOINTS.MASTERY.MY_AVERAGE
        );
        return response.data;
    },

    getByStudent: async (studentId: string): Promise<SkillMastery[]> => {
        const response = await apiClient.get<ApiResponse<SkillMastery[]>>(
            ENDPOINTS.MASTERY.BY_STUDENT(studentId)
        );
        return response.data;
    },

    getStudentAverage: async (studentId: string): Promise<number> => {
        const response = await apiClient.get<ApiResponse<number>>(
            ENDPOINTS.MASTERY.STUDENT_AVERAGE(studentId)
        );
        return response.data;
    },
};