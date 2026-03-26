import { cn } from '../../utils/cn';

interface ProgressProps {
    value: number;
    max?: number;
    size?: 'sm' | 'md' | 'lg';
    showLabel?: boolean;
    variant?: 'default' | 'success' | 'warning' | 'danger';
    className?: string;
}

const heights = {
    sm: 'h-1',
    md: 'h-2',
    lg: 'h-3',
};

const colors = {
    default: 'bg-zinc-600 dark:bg-zinc-400',
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    danger: 'bg-red-500',
};

export function Progress({
    value,
    max = 100,
    size = 'md',
    showLabel = false,
    variant = 'default',
    className,
}: ProgressProps) {
    const percentage = Math.min(Math.max((value / max) * 100, 0), 100);

    return (
        <div className={cn('w-full', className)}>
            {showLabel && (
                <div className="flex justify-between text-sm mb-1">
                    <span className="text-zinc-600 dark:text-zinc-400">{Math.round(percentage)}%</span>
                </div>
            )}
            <div
                className={cn(
                    'w-full bg-zinc-200 dark:bg-zinc-700 rounded-full overflow-hidden',
                    heights[size]
                )}
            >
                <div
                    className={cn('h-full rounded-full transition-all duration-300', colors[variant])}
                    style={{ width: `${percentage}%` }}
                />
            </div>
        </div>
    );
}

interface SkillProgressProps {
    name: string;
    value: number;
    className?: string;
}

export function SkillProgress({ name, value, className }: SkillProgressProps) {
    const percentage = Math.round(value * 100);
    
    const variant: ProgressProps['variant'] = 
        percentage >= 70 ? 'success' : 
        percentage >= 40 ? 'warning' : 'danger';

    return (
        <div className={cn('space-y-1', className)}>
            <div className="flex justify-between text-sm">
                <span className="font-medium text-zinc-700 dark:text-zinc-300">{name}</span>
                <span className="text-zinc-500 dark:text-zinc-400">{percentage}%</span>
            </div>
            <Progress value={percentage} variant={variant} size="sm" />
        </div>
    );
}
