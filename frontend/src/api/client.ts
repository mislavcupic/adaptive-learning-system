// API Client - koristi fetch API za HTTP komunikaciju
import { getAccessToken, clearTokens } from '../utils/storage';

// Čitaj API URL iz environment varijable
const BASE_URL = import.meta.env.VITE_API_URL || '/api';

interface RequestOptions extends RequestInit {
    skipAuth?: boolean;
}

class ApiError extends Error {
    status: number;
    data: unknown;

    constructor(message: string, status: number, data?: unknown) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
    }
}

async function request<T>(
    endpoint: string,
    options: RequestOptions = {}
): Promise<T> {
    const { skipAuth = false, ...fetchOptions } = options;

    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...fetchOptions.headers,
    };

    // Dodaj Authorization header ako nije skipAuth
    if (!skipAuth) {
        const token = getAccessToken();
        if (token) {
            (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
        }
    }

    const response = await fetch(`${BASE_URL}${endpoint}`, {
        ...fetchOptions,
        headers,
    });

    // Ako je 401 Unauthorized, očisti tokene i preusmjeri na login
    if (response.status === 401 && !skipAuth) {
        clearTokens();
        window.location.href = '/login';
        throw new ApiError('Unauthorized', 401);
    }

    // Ako response nije OK, baci grešku
    if (!response.ok) {
        let errorData;
        try {
            errorData = await response.json();
        } catch {
            errorData = null;
        }
        throw new ApiError(
            errorData?.message || `HTTP error: ${response.status}`,
            response.status,
            errorData
        );
    }

    // Ako nema sadržaja (204 No Content), vrati null
    if (response.status === 204) {
        return null as T;
    }

    return response.json();
}

// API Client s autentikacijom (za zaštićene rute)
export const apiClient = {
    get: <T>(url: string, options?: RequestOptions) =>
        request<T>(url, { ...options, method: 'GET' }),

    post: <T>(url: string, data?: unknown, options?: RequestOptions) =>
        request<T>(url, {
            ...options,
            method: 'POST',
            body: data ? JSON.stringify(data) : undefined,
        }),

    put: <T>(url: string, data?: unknown, options?: RequestOptions) =>
        request<T>(url, {
            ...options,
            method: 'PUT',
            body: data ? JSON.stringify(data) : undefined,
        }),

    patch: <T>(url: string, data?: unknown, options?: RequestOptions) =>
        request<T>(url, {
            ...options,
            method: 'PATCH',
            body: data ? JSON.stringify(data) : undefined,
        }),

    delete: <T>(url: string, options?: RequestOptions) =>
        request<T>(url, { ...options, method: 'DELETE' }),
};

// Public API bez autentikacije (za login, register)
export const publicApi = {
    get: <T>(url: string) =>
        request<T>(url, { method: 'GET', skipAuth: true }),

    post: <T>(url: string, data?: unknown) =>
        request<T>(url, {
            method: 'POST',
            body: data ? JSON.stringify(data) : undefined,
            skipAuth: true,
        }),
};

export const uploadFile = async <T>(
    url: string,
    formData: FormData
): Promise<T> => {
    const token = getAccessToken();

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: {
            ...(token && { Authorization: `Bearer ${token}` }),
        },
        body: formData,
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        throw new ApiError(
            errorData?.message || `HTTP error: ${response.status}`,
            response.status,
            errorData
        );
    }

    return response.json();
};
export { ApiError };
export default apiClient;