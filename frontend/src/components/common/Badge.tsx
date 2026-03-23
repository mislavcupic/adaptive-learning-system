type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info';
type BadgeSize = 'sm' | 'md';

interface BadgeProps {
    children: React.ReactNode;
    variant?: BadgeVariant;
    size?: BadgeSize;
    className?: string;
}

export const Badge = ({
                          children,
                          variant = 'default',
                          size = 'md',
                          className = '',
                      }: BadgeProps) => {
    const variantStyles: Record<BadgeVariant, string> = {
        default: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
        primary: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300',
        success: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
        warning: 'bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-300',
        danger: 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
        info: 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
    };

    const sizeStyles: Record<BadgeSize, string> = {
        sm: 'px-2 py-0.5 text-xs',
        md: 'px-2.5 py-1 text-sm',
    };

    return (
        <span
            className={`
        inline-flex items-center font-medium rounded-full
        ${variantStyles[variant]}
        ${sizeStyles[size]}
        ${className}
      `}
        >
      {children}
    </span>
    );
};