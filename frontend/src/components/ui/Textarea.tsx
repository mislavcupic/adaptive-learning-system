import { TextareaHTMLAttributes, forwardRef } from 'react';
import { cn } from '../../utils/cn';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    label?: string;
    error?: string;
    hint?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
    ({ className, label, error, hint, id, ...props }, ref) => {
        const textareaId = id || props.name;

        return (
            <div className="space-y-1.5">
                {label && (
                    <label
                        htmlFor={textareaId}
                        className="block text-sm font-medium text-zinc-700 dark:text-zinc-300"
                    >
                        {label}
                    </label>
                )}
                <textarea
                    ref={ref}
                    id={textareaId}
                    className={cn(
                        'w-full px-3 py-2 rounded-lg border bg-white dark:bg-zinc-900',
                        'text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 dark:placeholder-zinc-500',
                        'transition-colors duration-150 resize-y min-h-[100px]',
                        'focus:outline-none focus:ring-2 focus:ring-zinc-400 dark:focus:ring-zinc-500',
                        error
                            ? 'border-red-500 focus:ring-red-400'
                            : 'border-zinc-200 dark:border-zinc-700',
                        'disabled:opacity-50 disabled:cursor-not-allowed',
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

Textarea.displayName = 'Textarea';
