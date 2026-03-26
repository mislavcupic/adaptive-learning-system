import { apiClient, ENDPOINTS } from '../api';
import type { 
    ApiResponse, 
    StudentDashboardData, 
    TeacherDashboardData, 
    AdminDashboardData 
} from '../types';

export const dashboardService = {
    getStudentDashboard: async (): Promise<StudentDashboardData> => {
        const response = await apiClient.get<ApiResponse<StudentDashboardData>>(
            ENDPOINTS.DASHBOARD.STUDENT
        );
        return response.data;
    },

    getTeacherDashboard: async (): Promise<TeacherDashboardData> => {
        const response = await apiClient.get<ApiResponse<TeacherDashboardData>>(
            ENDPOINTS.DASHBOARD.TEACHER
        );
        return response.data;
    },

    getAdminDashboard: async (): Promise<AdminDashboardData> => {
        const response = await apiClient.get<ApiResponse<AdminDashboardData>>(
            ENDPOINTS.DASHBOARD.ADMIN
        );
        return response.data;
    },
};
