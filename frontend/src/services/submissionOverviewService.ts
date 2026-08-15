import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, SubmissionsOverview } from '../types';

export const submissionOverviewService = {
    getOverview: async (filters?: {
        courseId?: string;
        studentId?: string;
        lastDays?: number;
    }): Promise<SubmissionsOverview> => {
        const params: Record<string, string> = {};
        if (filters?.courseId) params.courseId = filters.courseId;
        if (filters?.studentId) params.studentId = filters.studentId;
        if (filters?.lastDays) params.lastDays = String(filters.lastDays);

        const response = await apiClient.get<ApiResponse<SubmissionsOverview>>(
            ENDPOINTS.TEACHER.SUBMISSIONS_OVERVIEW,
            { params }
        );
        return response.data;
    },
};