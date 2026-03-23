const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

// COOKIES (za refresh token - httpOnly u produkciji)
const setCookie = (name: string, value: string, days: number = 7) => {
    const expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/;SameSite=Strict`;
};

const getCookie = (name: string): string | null => {
    const nameEQ = `${name}=`;
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
        // Use a new constant or check the trimmed version directly
        const trimmedCookie = cookie.trim();
        if (trimmedCookie.startsWith(nameEQ)) {
            return trimmedCookie.substring(nameEQ.length);
        }
    }
    return null;
};

const deleteCookie = (name: string) => {
    document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
};

// ============ TOKEN MANAGEMENT ============
export const getAccessToken = (): string | null => {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
};

export const getRefreshToken = (): string | null => {
    return getCookie(REFRESH_TOKEN_KEY);
};

export const setTokens = (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    setCookie(REFRESH_TOKEN_KEY, refreshToken, 7); // 7 dana
};

export const clearTokens = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    deleteCookie(REFRESH_TOKEN_KEY);
};

// ============ GENERIC STORAGE ============
export const storage = {
    get: <T>(key: string): T | null => {
        const item = localStorage.getItem(key);
        if (!item) return null;
        try {
            return JSON.parse(item);
        } catch {
            return item as unknown as T;
        }
    },

    set: <T>(key: string, value: T): void => {
        localStorage.setItem(key, JSON.stringify(value));
    },

    remove: (key: string): void => {
        localStorage.removeItem(key);
    },

    clear: (): void => {
        localStorage.clear();
    },
};