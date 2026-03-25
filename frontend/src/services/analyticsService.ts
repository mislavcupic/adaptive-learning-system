import { apiClient, ENDPOINTS } from '../api';
import type { ClassStatistics, StudentDetailedProgress, CommonIssue, WeeklyProgress, SkillMastery } from '../types';

export const analyticsService = {
    getClassStatistics: async (classId: string): Promise<ClassStatistics> => {
        return await apiClient.get<ClassStatistics>(`${ENDPOINTS.CLASSES.BY_ID(classId)}/statistics`);
    },

    getStudentProgress: async (studentId: string): Promise<StudentDetailedProgress> => {
        return await apiClient.get<StudentDetailedProgress>(`/analytics/students/${studentId}/progress`);
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

        return await apiClient.get<CommonIssue[]>(`/analytics/common-issues?${queryParams}`);
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

        return await apiClient.get<WeeklyProgress[]>(`/analytics/weekly-progress?${queryParams}`);
    },

    getGroupComparison: async (classId: string): Promise<{
        experimental: { avgMastery: number; avgScore: number; submissionCount: number; completionRate: number };
        control: { avgMastery: number; avgScore: number; submissionCount: number; completionRate: number };
        difference: { mastery: number; score: number; completion: number };
    }> => {
        return await apiClient.get(`/analytics/classes/${classId}/group-comparison`);
    },

    getSkillMastery: async (studentId: string): Promise<SkillMastery[]> => {
        return await apiClient.get<SkillMastery[]>(`/analytics/students/${studentId}/skills`);
    },
};