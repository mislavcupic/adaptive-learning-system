import { SelectHTMLAttributes, forwardRef } from 'react';
import { cn } from '../../utils/cn';

interface SelectOption {
    value: string;
    label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
    label?: string;
    error?: string;
    options: SelectOption[];
    placeholder?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
    ({ className, label, error, options, placeholder, id, ...props }, ref) => {
        const selectId = id || props.name;

        return (
            <div className="space-y-1.5">
                {label && (
                    <label
                        htmlFor={selectId}
                        className="block text-sm font-medium text-zinc-700 dark:text-zinc-300"
                    >
                        {label}
                    </label>
                )}
                <select
                    ref={ref}
                    id={selectId}
                    className={cn(
                        'w-full px-3 py-2 rounded-lg border bg-white dark:bg-zinc-900',
                        'text-zinc-900 dark:text-zinc-100',
                        'transition-colors duration-150',
                        'focus:outline-none focus:ring-2 focus:ring-zinc-400 dark:focus:ring-zinc-500',
                        error
                            ? 'border-red-500 focus:ring-red-400'
                            : 'border-zinc-200 dark:border-zinc-700',
                        'disabled:opacity-50 disabled:cursor-not-allowed',
                        className
                    )}
                    {...props}
                >
                    {placeholder && (
                        <option value="" disabled>
                            {placeholder}
                        </option>
                    )}
                    {options.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
                {error && (
                    <p className="text-sm text-red-500">{error}</p>
                )}
            </div>
        );
    }
);

Select.displayName = 'Select';
