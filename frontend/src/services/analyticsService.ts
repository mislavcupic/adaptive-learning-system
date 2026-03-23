// ANALYTICS SERVICE - Statistike, grafovi, usporedba


import { api, ENDPOINTS } from '../api';
import type {
    ClassStatistics,
    StudentDetailedProgress,
    CommonIssue,
    WeeklyProgress,
    SkillMastery,
} from '../types';

export const analyticsService = {
    /**
     * get kompletne statistike razreda
     */
    getClassStatistics: async (classId: string): Promise<ClassStatistics> => {
        const response = await api.get<ClassStatistics>(
            ENDPOINTS.ANALYTICS.CLASS_STATS(classId)
        );
        return response.data;
    },

    /**
     * get detaljan napredak studenta
     */
    getStudentProgress: async (studentId: string): Promise<StudentDetailedProgress> => {
        const response = await api.get<StudentDetailedProgress>(
            ENDPOINTS.ANALYTICS.STUDENT_PROGRESS(studentId)
        );
        return response.data;
    },

    /**
     * get najčešće probleme u razredu
     */
    getCommonIssues: async (
        classId: string,
        params?: {
            limit?: number;
            groupType?: 'EXPERIMENTAL' | 'CONTROL';
        }
    ): Promise<CommonIssue[]> => {
        const queryParams = new URLSearchParams();
        if (params?.limit) queryParams.append('limit', params.limit.toString());
        if (params?.groupType) queryParams.append('groupType', params.groupType);

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.ANALYTICS.COMMON_ISSUES(classId)}?${queryParams}`
            : ENDPOINTS.ANALYTICS.COMMON_ISSUES(classId);

        const response = await api.get<CommonIssue[]>(endpoint);
        return response.data;
    },

    /**
     * get tjedni napredak razreda
     */
    getWeeklyProgress: async (
        classId: string,
        params?: {
            weeks?: number;
        }
    ): Promise<WeeklyProgress[]> => {
        const queryParams = new URLSearchParams();
        if (params?.weeks) queryParams.append('weeks', params.weeks.toString());

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.ANALYTICS.WEEKLY_PROGRESS(classId)}?${queryParams}`
            : ENDPOINTS.ANALYTICS.WEEKLY_PROGRESS(classId);

        const response = await api.get<WeeklyProgress[]>(endpoint);
        return response.data;
    },

    /**
     * Usporedi EXPERIMENTAL vs CONTROL grupu
     */
    compareGroups: async (classId: string): Promise<{
        experimental: {
            avgMastery: number;
            avgScore: number;
            submissionCount: number;
            completionRate: number;
        };
        control: {
            avgMastery: number;
            avgScore: number;
            submissionCount: number;
            completionRate: number;
        };
        difference: {
            masteryDiff: number;
            scoreDiff: number;
            significanceLevel: number;
        };
    }> => {
        const response = await api.get<{
            experimental: {
                avgMastery: number;
                avgScore: number;
                submissionCount: number;
                completionRate: number;
            };
            control: {
                avgMastery: number;
                avgScore: number;
                submissionCount: number;
                completionRate: number;
            };
            difference: {
                masteryDiff: number;
                scoreDiff: number;
                significanceLevel: number;
            };
        }>(ENDPOINTS.ANALYTICS.COMPARE_GROUPS(classId));
        return response.data;
    },

    /**
     * get skill mastery za studenta
     */
    getStudentSkills: async (studentId: string): Promise<SkillMastery[]> => {
        const response = await api.get<SkillMastery[]>(
            ENDPOINTS.SKILLS.BY_STUDENT(studentId)
        );
        return response.data;
    },
};