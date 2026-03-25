import { apiClient, ENDPOINTS } from '../api';
import { setTokens, clearTokens } from '../utils/storage';
import type { LoginCredentials, AuthResponse, User } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

export const authService = {
    login: async (credentials: LoginCredentials): Promise<User> => {
        const response = await apiClient.post<ApiResponse<AuthResponse>>(
            ENDPOINTS.AUTH.LOGIN,
            credentials,
            { skipAuth: true }
        );
        const { user, accessToken, refreshToken } = response.data;
        setTokens(accessToken, refreshToken);
        return user;
    },

    logout: async (): Promise<void> => {
        try {
            await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
        } finally {
            clearTokens();
        }
    },

    refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
        const response = await apiClient.post<ApiResponse<AuthResponse>>(
            ENDPOINTS.AUTH.REFRESH,
            { refreshToken },
            { skipAuth: true }
        );
        return response.data;
    },

    me: async (): Promise<User> => {
        const response = await apiClient.get<ApiResponse<User>>(ENDPOINTS.AUTH.ME);
        return response.data;
    },

    getCurrentUser: async (): Promise<User> => {
        const response = await apiClient.get<ApiResponse<User>>(ENDPOINTS.AUTH.ME);
        return response.data;
    },
};