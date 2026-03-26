import { HTMLAttributes, forwardRef } from 'react';
import { cn } from '../../utils/cn';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
    hoverable?: boolean;
}

export const Card = forwardRef<HTMLDivElement, CardProps>(
    ({ className, hoverable = false, ...props }, ref) => {
        return (
            <div
                ref={ref}
                className={cn(
                    'bg-white dark:bg-zinc-900 rounded-xl border border-zinc-200 dark:border-zinc-800',
                    hoverable && 'transition-shadow hover:shadow-lg cursor-pointer',
                    className
                )}
                {...props}
            />
        );
    }
);

Card.displayName = 'Card';

export const CardHeader = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
    ({ className, ...props }, ref) => {
        return (
            <div
                ref={ref}
                className={cn('px-6 py-4 border-b border-zinc-100 dark:border-zinc-800', className)}
                {...props}
            />
        );
    }
);

CardHeader.displayName = 'CardHeader';

export const CardTitle = forwardRef<HTMLHeadingElement, HTMLAttributes<HTMLHeadingElement>>(
    ({ className, ...props }, ref) => {
        return (
            <h3
                ref={ref}
                className={cn('text-lg font-semibold text-zinc-900 dark:text-zinc-100', className)}
                {...props}
            />
        );
    }
);

CardTitle.displayName = 'CardTitle';

export const CardDescription = forwardRef<HTMLParagraphElement, HTMLAttributes<HTMLParagraphElement>>(
    ({ className, ...props }, ref) => {
        return (
            <p
                ref={ref}
                className={cn('text-sm text-zinc-500 dark:text-zinc-400 mt-1', className)}
                {...props}
            />
        );
    }
);

CardDescription.displayName = 'CardDescription';

export const CardContent = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
    ({ className, ...props }, ref) => {
        return (
            <div
                ref={ref}
                className={cn('px-6 py-4', className)}
                {...props}
            />
        );
    }
);

CardContent.displayName = 'CardContent';

export const CardFooter = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
    ({ className, ...props }, ref) => {
        return (
            <div
                ref={ref}
                className={cn(
                    'px-6 py-4 border-t border-zinc-100 dark:border-zinc-800 flex items-center gap-3',
                    className
                )}
                {...props}
            />
        );
    }
);

CardFooter.displayName = 'CardFooter';
