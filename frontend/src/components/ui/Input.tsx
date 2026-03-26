import { InputHTMLAttributes, forwardRef } from 'react';
import { cn } from '../../utils/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    label?: string;
    error?: string;
    hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
    ({ className, label, error, hint, id, ...props }, ref) => {
        const inputId = id || props.name;

        return (
            <div className="space-y-1.5">
                {label && (
                    <label
                        htmlFor={inputId}
                        className="block text-sm font-medium text-zinc-700 dark:text-zinc-300"
                    >
                        {label}
                    </label>
                )}
                <input
                    ref={ref}
                    id={inputId}
                    className={cn(
                        'w-full px-3 py-2 rounded-lg border bg-white dark:bg-zinc-900',
                        'text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 dark:placeholder-zinc-500',
                        'transition-colors duration-150',
                        'focus:outline-none focus:ring-2 focus:ring-zinc-400 dark:focus:ring-zinc-500',
                        error
                            ? 'border-red-500 focus:ring-red-400'
                            : 'border-zinc-200 dark:border-zinc-700',
                        'disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-zinc-50 dark:disabled:bg-zinc-800',
                        className
                    )}
                    {...props}
                />
                {error && (
                    <p className="text-sm text-red-500">{error}</p>
                )}
                {hint && !error && (
                    <p className="text-sm text-zinc-500 dark:text-zinc-400">{hint}</p>
                )}
            </div>
        );
    }
);

Input.displayName = 'Input';
