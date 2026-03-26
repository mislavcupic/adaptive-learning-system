import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, AuthResponse, LoginCredentials, RegisterRequest, User } from '../types';

export const authService = {
    login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
        const response = await apiClient.post<ApiResponse<AuthResponse>>(
            ENDPOINTS.AUTH.LOGIN, 
            credentials
        );
        
        if (response.data) {
            localStorage.setItem('accessToken', response.data.accessToken);
            localStorage.setItem('refreshToken', response.data.refreshToken);
            localStorage.setItem('user', JSON.stringify(response.data.user));
        }
        
        return response.data;
    },

    register: async (data: RegisterRequest): Promise<AuthResponse> => {
        const response = await apiClient.post<ApiResponse<AuthResponse>>(
            ENDPOINTS.AUTH.REGISTER, 
            data
        );
        return response.data;
    },

    logout: async (): Promise<void> => {
        try {
            await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
        } finally {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
        }
    },

    getCurrentUser: async (): Promise<User> => {
        const response = await apiClient.get<ApiResponse<User>>(ENDPOINTS.AUTH.ME);
        return response.data;
    },

    getStoredUser: (): User | null => {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    },

    isAuthenticated: (): boolean => {
        return !!localStorage.getItem('accessToken');
    },

    getAccessToken: (): string | null => {
        return localStorage.getItem('accessToken');
    },
};
