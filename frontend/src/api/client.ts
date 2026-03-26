import { API_BASE_URL } from './endpoints';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface RequestOptions {
    headers?: Record<string, string>;
    params?: Record<string, string>;
}

const getAuthToken = (): string | null => {
    return localStorage.getItem('accessToken');
};

const buildUrl = (endpoint: string, params?: Record<string, string>): string => {
    const url = new URL(`${API_BASE_URL}${endpoint}`);
    if (params) {
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                url.searchParams.append(key, value);
            }
        });
    }
    return url.toString();
};

const request = async <T>(
    method: HttpMethod,
    endpoint: string,
    data?: unknown,
    options?: RequestOptions
): Promise<T> => {
    const token = getAuthToken();
    
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...options?.headers,
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const config: RequestInit = {
        method,
        headers,
    };
    
    if (data && method !== 'GET') {
        config.body = JSON.stringify(data);
    }
    
    const url = buildUrl(endpoint, options?.params);
    
    const response = await fetch(url, config);
    
    if (response.status === 401) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }
    
    const json = await response.json();
    
    if (!response.ok) {
        throw new Error(json.message || `HTTP Error: ${response.status}`);
    }
    
    return json as T;
};

export const apiClient = {
    get: <T>(endpoint: string, options?: RequestOptions) => 
        request<T>('GET', endpoint, undefined, options),
    
    post: <T>(endpoint: string, data?: unknown, options?: RequestOptions) => 
        request<T>('POST', endpoint, data, options),
    
    put: <T>(endpoint: string, data?: unknown, options?: RequestOptions) => 
        request<T>('PUT', endpoint, data, options),
    
    patch: <T>(endpoint: string, data?: unknown, options?: RequestOptions) => 
        request<T>('PATCH', endpoint, data, options),
    
    delete: <T>(endpoint: string, options?: RequestOptions) => 
        request<T>('DELETE', endpoint, undefined, options),
};

export { ENDPOINTS } from './endpoints';
