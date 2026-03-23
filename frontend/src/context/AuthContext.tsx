// ============================================
// AUTH CONTEXT - Globalni auth state
// ============================================

import {
    createContext,
    useContext,
    useState,
    useEffect,
    useCallback,
    type ReactNode,
} from 'react';
import { authService } from '../services';
import { getAccessToken, clearTokens } from '../utils/storage';
import type { User, LoginCredentials, AuthState } from '../types';

// ============ CONTEXT TIPOVI ============
interface AuthContextType extends AuthState {
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
}

// ============ CONTEXT ============
const AuthContext = createContext<AuthContextType | null>(null);

// ============ PROVIDER ============
interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const isAuthenticated = !!user;

    /**
     * Provjeri token i dohvati korisnika pri učitavanju
     */
    const initializeAuth = useCallback(async () => {
        const token = getAccessToken();

        if (!token) {
            setIsLoading(false);
            return;
        }

        try {
            const currentUser = await authService.getCurrentUser();
            setUser(currentUser);
        } catch (error) {
            clearTokens();
            setUser(null);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        initializeAuth();
    }, [initializeAuth]);

    /**
     * Login korisnika
     */
    const login = async (credentials: LoginCredentials) => {
        setIsLoading(true);
        try {
            const loggedInUser = await authService.login(credentials);
            setUser(loggedInUser);
        } finally {
            setIsLoading(false);
        }
    };

    /**
     * Logout korisnika
     */
    const logout = async () => {
        setIsLoading(true);
        try {
            await authService.logout();
        } finally {
            setUser(null);
            setIsLoading(false);
        }
    };

    /**
     * refresh podataka o korisniku
     */
    const refreshUser = async () => {
        try {
            const currentUser = await authService.getCurrentUser();
            setUser(currentUser);
        } catch (error) {
            clearTokens();
            setUser(null);
        }
    };

    const value: AuthContextType = {
        user,
        isAuthenticated,
        isLoading,
        login,
        logout,
        refreshUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};


export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }

    return context;
};