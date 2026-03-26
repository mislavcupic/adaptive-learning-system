import { cn } from '../../utils/cn';
import { getInitials } from '../../utils/formatters';

interface AvatarProps {
    firstName?: string;
    lastName?: string;
    src?: string;
    size?: 'sm' | 'md' | 'lg';
    className?: string;
}

const sizes = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-12 h-12 text-base',
};

export function Avatar({ firstName, lastName, src, size = 'md', className }: AvatarProps) {
    const initials = getInitials(firstName, lastName);

    if (src) {
        return (
            <img
                src={src}
                alt={`${firstName} ${lastName}`}
                className={cn('rounded-full object-cover', sizes[size], className)}
            />
        );
    }

    return (
        <div
            className={cn(
                'rounded-full bg-zinc-200 dark:bg-zinc-700 flex items-center justify-center',
                'font-medium text-zinc-600 dark:text-zinc-300',
                sizes[size],
                className
            )}
        >
            {initials}
        </div>
    );
}
