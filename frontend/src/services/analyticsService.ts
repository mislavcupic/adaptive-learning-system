import { apiClient, ENDPOINTS } from '../api';
import type { ClassStatistics, StudentDetailedProgress, CommonIssue, WeeklyProgress, SkillMastery } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

interface GroupComparison {
    experimental: { avgMastery: number; avgScore: number; submissionCount: number; completionRate: number };
    control: { avgMastery: number; avgScore: number; submissionCount: number; completionRate: number };
    difference: { mastery: number; score: number; completion: number };
}

export const analyticsService = {
    getClassStatistics: async (classId: string): Promise<ClassStatistics> => {
        const response = await apiClient.get<ApiResponse<ClassStatistics>>(
            `${ENDPOINTS.CLASSES.BY_ID(classId)}/statistics`
        );
        return response.data;
    },

    getStudentProgress: async (studentId: string): Promise<StudentDetailedProgress> => {
        const response = await apiClient.get<ApiResponse<StudentDetailedProgress>>(
            `/analytics/students/${studentId}/progress`
        );
        return response.data;
    },

    getCommonIssues: async (params?: {
        classId?: string;
        courseId?: string;
        limit?: number;
    }): Promise<CommonIssue[]> => {
        const queryParams = new URLSearchParams();
        if (params?.classId) queryParams.append('classId', params.classId);
        if (params?.courseId) queryParams.append('courseId', params.courseId);
        if (params?.limit) queryParams.append('limit', params.limit.toString());

        const response = await apiClient.get<ApiResponse<CommonIssue[]>>(
            `/analytics/common-issues?${queryParams}`
        );
        return response.data;
    },

    getWeeklyProgress: async (params?: {
        classId?: string;
        studentId?: string;
        weeks?: number;
    }): Promise<WeeklyProgress[]> => {
        const queryParams = new URLSearchParams();
        if (params?.classId) queryParams.append('classId', params.classId);
        if (params?.studentId) queryParams.append('studentId', params.studentId);
        if (params?.weeks) queryParams.append('weeks', params.weeks.toString());

        const response = await apiClient.get<ApiResponse<WeeklyProgress[]>>(
            `/analytics/weekly-progress?${queryParams}`
        );
        return response.data;
    },

    getGroupComparison: async (classId: string): Promise<GroupComparison> => {
        const response = await apiClient.get<ApiResponse<GroupComparison>>(
            `/analytics/classes/${classId}/group-comparison`
        );
        return response.data;
    },

    getSkillMastery: async (studentId: string): Promise<SkillMastery[]> => {
        const response = await apiClient.get<ApiResponse<SkillMastery[]>>(
            `/analytics/students/${studentId}/skills`
        );
        return response.data;
    },
};
