import { apiClient } from '../api';
import type { 
    StudentDashboardData, 
    TeacherDashboardData, 
    AdminDashboardData 
} from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const dashboardService = {
    getStudentDashboard: async (): Promise<StudentDashboardData> => {
        const response = await apiClient.get<ApiResponse<StudentDashboardData>>('/dashboard/student');
        return response.data;
    },

    getTeacherDashboard: async (): Promise<TeacherDashboardData> => {
        const response = await apiClient.get<ApiResponse<TeacherDashboardData>>('/dashboard/teacher');
        return response.data;
    },

    getAdminDashboard: async (): Promise<AdminDashboardData> => {
        const response = await apiClient.get<ApiResponse<AdminDashboardData>>('/dashboard/admin');
        return response.data;
    },
};
