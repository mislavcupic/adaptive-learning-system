
// API CLIENT - Fetch wrapper s JWT logikom


import { getAccessToken, getRefreshToken, setTokens, clearTokens } from '../utils/storage';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// tipovi
interface ApiResponse<T> {
    data: T;
    status: number;
    ok: boolean;
}

interface ApiError {
    message: string;
    status: number;
    errors?: Record<string, string[]>;
}

interface RefreshResponse {
    accessToken: string;
    refreshToken: string;
}

// stanje
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

// Kad refresh završi, obavijestim sve subscribere
const onRefreshed = (newToken: string) => {
    refreshSubscribers.forEach((callback) => callback(newToken));
    refreshSubscribers = [];
};

// Dodaj request u queue dok čekamo refresh
const addRefreshSubscriber = (callback: (token: string) => void) => {
    refreshSubscribers.push(callback);
};

// REFRESH TOKEN
const refreshAccessToken = async (): Promise<string | null> => {
    const refreshToken = getRefreshToken();

    if (!refreshToken) {
        clearTokens();
        globalThis.location.href = '/login';
        return null;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken }),
        });

        if (!response.ok) {
            throw new Error('Refresh failed');
        }

        const data: RefreshResponse = await response.json();
        setTokens(data.accessToken, data.refreshToken);
        return data.accessToken;
    } catch (error) {
        clearTokens();
        globalThis.location.href = '/login';
        return null;
    }
};

// gl.fetch wrapper
const fetchWithAuth = async <T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<ApiResponse<T>> => {
    const url = `${API_BASE_URL}${endpoint}`;
    let accessToken = getAccessToken();

    // Pripremi headers
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // Dodajem Authorization header ako imam token
    if (accessToken) {
        (headers as Record<string, string>)['Authorization'] = `Bearer ${accessToken}`;
    }

    // Prvi pokušaj
    let response = await fetch(url, {
        ...options,
        headers,
    });

    // Ako je 401, pokušavam refresh
    if (response.status === 401 && accessToken) {
        if (!isRefreshing) {
            isRefreshing = true;

            const newToken = await refreshAccessToken();
            isRefreshing = false;

            if (newToken) {
                onRefreshed(newToken);

                // Ponovi originalni request s novim tokenom
                (headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`;
                response = await fetch(url, {
                    ...options,
                    headers,
                });
            }
        } else {
            // Refresh je u tijeku, čekaj
            return new Promise((resolve) => {
                addRefreshSubscriber(async (newToken: string) => {
                    (headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`;
                    const retryResponse = await fetch(url, {
                        ...options,
                        headers,
                    });
                    const data = await retryResponse.json();
                    resolve({
                        data,
                        status: retryResponse.status,
                        ok: retryResponse.ok,
                    });
                });
            });
        }
    }

    // Parse response
    const data = response.status === 204 ? null:await response.json();

    if (!response.ok) {
        const error: ApiError = {
            message: data?.message || 'Došlo je do greške',
            status: response.status,
            errors: data?.errors,
        };
        throw error;
    }

    return {
        data,
        status: response.status,
        ok: response.ok,
    };
};

// FETCH BEZ AUTHA (za login, register)
const fetchPublic = async <T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<ApiResponse<T>> => {
    const url = `${API_BASE_URL}${endpoint}`;

    const response = await fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    });

    const data = response.status === 204 ? null: await response.json();

    if (!response.ok) {
        const error: ApiError = {
            message: data?.message || 'Došlo je do greške',
            status: response.status,
            errors: data?.errors,
        };
        throw error;
    }

    return {
        data,
        status: response.status,
        ok: response.ok,
    };
};

// helper metode
export const api = {
    get: <T>(endpoint: string) =>
        fetchWithAuth<T>(endpoint, { method: 'GET' }),

    post: <T>(endpoint: string, body?: unknown) =>
        fetchWithAuth<T>(endpoint, {
            method: 'POST',
            body: body ? JSON.stringify(body) : undefined,
        }),

    put: <T>(endpoint: string, body?: unknown) =>
        fetchWithAuth<T>(endpoint, {
            method: 'PUT',
            body: body ? JSON.stringify(body) : undefined,
        }),

    patch: <T>(endpoint: string, body?: unknown) =>
        fetchWithAuth<T>(endpoint, {
            method: 'PATCH',
            body: body ? JSON.stringify(body) : undefined,
        }),

    delete: <T>(endpoint: string) =>
        fetchWithAuth<T>(endpoint, { method: 'DELETE' }),

    // Za upload datoteka (CSV import)
    upload: <T>(endpoint: string, formData: FormData) =>
        fetchWithAuth<T>(endpoint, {
            method: 'POST',
            body: formData,
            headers: {}, // Bez Content-Type, browser će postaviti s boundary
        }),
};

// Public API (bez auth)
export const publicApi = {
    post: <T>(endpoint: string, body?: unknown) =>
        fetchPublic<T>(endpoint, {
            method: 'POST',
            body: body ? JSON.stringify(body) : undefined,
        }),

    get: <T>(endpoint: string) =>
        fetchPublic<T>(endpoint, { method: 'GET' }),
};

export type { ApiError, ApiResponse };