import { ReactNode } from 'react';
import { cn } from '../../utils/cn';

interface EmptyStateProps {
    icon?: ReactNode;
    title: string;
    description?: string;
    action?: ReactNode;
    className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
    return (
        <div className={cn('flex flex-col items-center justify-center py-12 px-4', className)}>
            {icon && (
                <div className="w-12 h-12 rounded-full bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center mb-4">
                    <div className="text-zinc-400 dark:text-zinc-500">{icon}</div>
                </div>
            )}
            <h3 className="text-lg font-medium text-zinc-900 dark:text-zinc-100 text-center">
                {title}
            </h3>
            {description && (
                <p className="text-sm text-zinc-500 dark:text-zinc-400 text-center mt-1 max-w-sm">
                    {description}
                </p>
            )}
            {action && <div className="mt-4">{action}</div>}
        </div>
    );
}

export function ErrorState({ 
    title = 'Došlo je do greške',
    description,
    onRetry,
}: {
    title?: string;
    description?: string;
    onRetry?: () => void;
}) {
    return (
        <div className="flex flex-col items-center justify-center py-12 px-4">
            <div className="w-12 h-12 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center mb-4">
                <svg
                    className="w-6 h-6 text-red-500"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                    />
                </svg>
            </div>
            <h3 className="text-lg font-medium text-zinc-900 dark:text-zinc-100 text-center">
                {title}
            </h3>
            {description && (
                <p className="text-sm text-zinc-500 dark:text-zinc-400 text-center mt-1 max-w-sm">
                    {description}
                </p>
            )}
            {onRetry && (
                <button
                    onClick={onRetry}
                    className="mt-4 px-4 py-2 text-sm font-medium text-zinc-700 dark:text-zinc-300 bg-zinc-100 dark:bg-zinc-800 rounded-lg hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
                >
                    Pokušaj ponovno
                </button>
            )}
        </div>
    );
}
