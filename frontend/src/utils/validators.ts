export type Validator<T> = (value: T) => string | null;

// Email validator
export const isEmail: Validator<string> = (value) => {
    if (!value) return null;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(value) ? null : 'Unesite valjanu email adresu';
};

// Required validator
export const required = (message = 'Ovo polje je obavezno'): Validator<unknown> => {
    return (value) => {
        if (value === null || value === undefined || value === '') {
            return message;
        }
        if (Array.isArray(value) && value.length === 0) {
            return message;
        }
        return null;
    };
};

// Min length validator
export const minLength = (min: number, message?: string): Validator<string> => {
    return (value) => {
        if (!value) return null;
        return value.length >= min 
            ? null 
            : message || `Minimalno ${min} znakova`;
    };
};

// Max length validator
export const maxLength = (max: number, message?: string): Validator<string> => {
    return (value) => {
        if (!value) return null;
        return value.length <= max 
            ? null 
            : message || `Maksimalno ${max} znakova`;
    };
};

// Password validator
export const isPassword: Validator<string> = (value) => {
    if (!value) return null;
    if (value.length < 8) return 'Lozinka mora imati najmanje 8 znakova';
    if (!/[A-Z]/.test(value)) return 'Lozinka mora sadržavati barem jedno veliko slovo';
    if (!/[a-z]/.test(value)) return 'Lozinka mora sadržavati barem jedno malo slovo';
    if (!/[0-9]/.test(value)) return 'Lozinka mora sadržavati barem jednu znamenku';
    return null;
};

// Match validator (for password confirmation)
export const matches = (fieldName: string, fieldValue: string, message?: string): Validator<string> => {
    return (value) => {
        if (!value) return null;
        return value === fieldValue 
            ? null 
            : message || `Mora se podudarati s poljem ${fieldName}`;
    };
};

// Number range validator
export const numberRange = (min: number, max: number, message?: string): Validator<number> => {
    return (value) => {
        if (value === null || value === undefined) return null;
        return value >= min && value <= max 
            ? null 
            : message || `Vrijednost mora biti između ${min} i ${max}`;
    };
};

// Combine multiple validators
export const compose = <T>(...validators: Validator<T>[]): Validator<T> => {
    return (value) => {
        for (const validator of validators) {
            const error = validator(value);
            if (error) return error;
        }
        return null;
    };
};

// Validate entire form
export const validateForm = <T extends Record<string, unknown>>(
    data: T,
    rules: Partial<Record<keyof T, Validator<unknown>>>
): Record<string, string> => {
    const errors: Record<string, string> = {};
    
    for (const [field, validator] of Object.entries(rules)) {
        if (validator) {
            const error = validator(data[field as keyof T]);
            if (error) {
                errors[field] = error;
            }
        }
    }
    
    return errors;
};
