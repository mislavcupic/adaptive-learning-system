// ============================================
// USE DEBOUNCE HOOK - Odgodi izvršavanje
// ============================================

import { useState, useEffect } from 'react';

/**
 * Debounce hook - odgađa promjenu vrijednosti
 *
 * @example
 * const [searchTerm, setSearchTerm] = useState('');
 * const debouncedSearch = useDebounce(searchTerm, 500);
 *
 * useEffect(() => {
 *   // Pozovi API samo kad korisnik prestane tipkati 500ms
 *   searchApi(debouncedSearch);
 * }, [debouncedSearch]);
 */
export function useDebounce<T>(value: T, delay: number = 500): T {
    const [debouncedValue, setDebouncedValue] = useState<T>(value);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedValue(value);
        }, delay);

        return () => {
            clearTimeout(timer);
        };
    }, [value, delay]);

    return debouncedValue;
}