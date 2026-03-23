// AUTH SERVICE - Login, logout, refresh, me
import { api, publicApi, ENDPOINTS } from '../api';
import { setTokens, clearTokens } from '../utils/storage';
import type { LoginCredentials, AuthResponse, User } from '../types';

export const authService = {
    /**
     * Login korisnika
     */
    login: async (credentials: LoginCredentials): Promise<User> => {
        const response = await publicApi.post<AuthResponse>(
            ENDPOINTS.AUTH.LOGIN,
            credentials
        );

        const { user, accessToken, refreshToken } = response.data;
        setTokens(accessToken, refreshToken);

        return user;
    },

    /**
     * Logout - briše tokene
     */
    logout: async (): Promise<void> => {
        try {
            await api.post(ENDPOINTS.AUTH.LOGOUT);
        } catch {
            // Ignoriraj grešku, svejedno brišemo tokene
        } finally {
            clearTokens();
        }
    },

    /**
     * Dohvati trenutnog korisnika
     */
    getCurrentUser: async (): Promise<User> => {
        const response = await api.get<User>(ENDPOINTS.AUTH.ME);
        return response.data;
    },

    /**
     * Registracija (ako bude potrebna)
     */
    register: async (data: {
        email: string;
        password: string;
        firstName: string;
        lastName: string;
    }): Promise<User> => {
        const response = await publicApi.post<AuthResponse>(
            ENDPOINTS.AUTH.REGISTER,
            data
        );

        const { user, accessToken, refreshToken } = response.data;
        setTokens(accessToken, refreshToken);

        return user;
    },
};