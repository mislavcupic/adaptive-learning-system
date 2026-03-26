import { useState, useEffect, useCallback } from 'react';

interface UseFetchOptions {
    immediate?: boolean;
}

interface UseFetchReturn<T> {
    data: T | null;
    loading: boolean;
    error: string | null;
    refetch: () => Promise<void>;
}

export function useFetch<T>(
    fetchFn: () => Promise<T>,
    deps: unknown[] = [],
    options: UseFetchOptions = { immediate: true }
): UseFetchReturn<T> {
    const [data, setData] = useState<T | null>(null);
    const [loading, setLoading] = useState(options.immediate ?? true);
    const [error, setError] = useState<string | null>(null);

    const fetch = useCallback(async () => {
        setLoading(true);
        setError(null);
        
        try {
            const result = await fetchFn();
            setData(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Došlo je do greške');
        } finally {
            setLoading(false);
        }
    }, [fetchFn]);

    useEffect(() => {
        if (options.immediate !== false) {
            fetch();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps);

    return { data, loading, error, refetch: fetch };
}
