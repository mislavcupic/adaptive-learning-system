import { useState, useCallback } from 'react';
import type { ApiError } from '../api';

interface UseApiState<T> {
    data: T | null;
    isLoading: boolean;
    error: ApiError | null;
}

interface UseApiReturn<T, P extends unknown[]> extends UseApiState<T> {
    execute: (...params: P) => Promise<T | null>;
    reset: () => void;
    setData: (data: T | null) => void;
}

/**
 * Generic hook za API pozive s loading/error stanjem
 *
 * @example
 * const { data, isLoading, error, execute } = useApi(courseService.getAll);
 *
 * useEffect(() => {
 *   execute();
 * }, []);
 */
export function useApi<T, P extends unknown[]>(
    apiFunction: (...params: P) => Promise<T>
): UseApiReturn<T, P> {
    const [state, setState] = useState<UseApiState<T>>({
        data: null,
        isLoading: false,
        error: null,
    });

    const execute = useCallback(
        async (...params: P): Promise<T | null> => {
            setState((prev) => ({ ...prev, isLoading: true, error: null }));

            try {
                const result = await apiFunction(...params);
                setState({ data: result, isLoading: false, error: null });
                return result;
            } catch (error) {
                const apiError = error as ApiError;
                setState({ data: null, isLoading: false, error: apiError });
                return null;
            }
        },
        [apiFunction]
    );

    const reset = useCallback(() => {
        setState({ data: null, isLoading: false, error: null });
    }, []);

    const setData = useCallback((data: T | null) => {
        setState((prev) => ({ ...prev, data }));
    }, []);

    return {
        ...state,
        execute,
        reset,
        setData,
    };
}