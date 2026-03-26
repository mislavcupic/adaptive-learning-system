import { useState, useCallback, ChangeEvent, FormEvent } from 'react';
import type { Validator } from '../utils/validators';

interface UseFormOptions<T> {
    initialValues: T;
    validators?: Partial<Record<keyof T, Validator<unknown>>>;
    onSubmit: (values: T) => Promise<void> | void;
}

interface UseFormReturn<T> {
    values: T;
    errors: Partial<Record<keyof T, string>>;
    touched: Partial<Record<keyof T, boolean>>;
    isSubmitting: boolean;
    isValid: boolean;
    handleChange: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void;
    handleBlur: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void;
    handleSubmit: (e: FormEvent) => Promise<void>;
    setFieldValue: (field: keyof T, value: T[keyof T]) => void;
    setFieldError: (field: keyof T, error: string) => void;
    reset: () => void;
    validate: () => boolean;
}

export function useForm<T extends Record<string, unknown>>({
    initialValues,
    validators = {},
    onSubmit,
}: UseFormOptions<T>): UseFormReturn<T> {
    const [values, setValues] = useState<T>(initialValues);
    const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});
    const [touched, setTouched] = useState<Partial<Record<keyof T, boolean>>>({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const validateField = useCallback(
        (field: keyof T, value: unknown): string | null => {
            const validator = validators[field];
            if (!validator) return null;
            return validator(value);
        },
        [validators]
    );

    const validate = useCallback((): boolean => {
        const newErrors: Partial<Record<keyof T, string>> = {};
        let isValid = true;

        for (const field of Object.keys(validators) as (keyof T)[]) {
            const error = validateField(field, values[field]);
            if (error) {
                newErrors[field] = error;
                isValid = false;
            }
        }

        setErrors(newErrors);
        return isValid;
    }, [validators, values, validateField]);

    const handleChange = useCallback(
        (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
            const { name, value, type } = e.target;
            const newValue = type === 'checkbox' 
                ? (e.target as HTMLInputElement).checked 
                : value;

            setValues((prev) => ({ ...prev, [name]: newValue }));

            // Validate on change if field was touched
            if (touched[name as keyof T]) {
                const error = validateField(name as keyof T, newValue);
                setErrors((prev) => ({
                    ...prev,
                    [name]: error || undefined,
                }));
            }
        },
        [touched, validateField]
    );

    const handleBlur = useCallback(
        (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
            const { name, value } = e.target;
            
            setTouched((prev) => ({ ...prev, [name]: true }));
            
            const error = validateField(name as keyof T, value);
            setErrors((prev) => ({
                ...prev,
                [name]: error || undefined,
            }));
        },
        [validateField]
    );

    const handleSubmit = useCallback(
        async (e: FormEvent) => {
            e.preventDefault();
            
            // Touch all fields
            const allTouched = Object.keys(initialValues).reduce(
                (acc, key) => ({ ...acc, [key]: true }),
                {} as Partial<Record<keyof T, boolean>>
            );
            setTouched(allTouched);

            if (!validate()) return;

            setIsSubmitting(true);
            try {
                await onSubmit(values);
            } finally {
                setIsSubmitting(false);
            }
        },
        [validate, onSubmit, values, initialValues]
    );

    const setFieldValue = useCallback((field: keyof T, value: T[keyof T]) => {
        setValues((prev) => ({ ...prev, [field]: value }));
    }, []);

    const setFieldError = useCallback((field: keyof T, error: string) => {
        setErrors((prev) => ({ ...prev, [field]: error }));
    }, []);

    const reset = useCallback(() => {
        setValues(initialValues);
        setErrors({});
        setTouched({});
        setIsSubmitting(false);
    }, [initialValues]);

    const isValid = Object.keys(errors).length === 0;

    return {
        values,
        errors,
        touched,
        isSubmitting,
        isValid,
        handleChange,
        handleBlur,
        handleSubmit,
        setFieldValue,
        setFieldError,
        reset,
        validate,
    };
}
