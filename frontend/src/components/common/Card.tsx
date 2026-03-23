interface CardProps {
    children: React.ReactNode;
    className?: string;
    padding?: 'none' | 'sm' | 'md' | 'lg';
}

interface CardHeaderProps {
    children: React.ReactNode;
    className?: string;
    action?: React.ReactNode;
}

interface CardTitleProps {
    children: React.ReactNode;
    className?: string;
}

interface CardContentProps {
    children: React.ReactNode;
    className?: string;
}

interface CardFooterProps {
    children: React.ReactNode;
    className?: string;
}

export const Card = ({ children, className = '', padding = 'md' }: CardProps) => {
    const paddingStyles = {
        none: '',
        sm: 'p-4',
        md: 'p-6',
        lg: 'p-8',
    };

    return (
        <div
            className={`
        bg-white dark:bg-gray-800 
        rounded-xl 
        border border-gray-200 dark:border-gray-700
        shadow-sm
        ${paddingStyles[padding]}
        ${className}
      `}
        >
            {children}
        </div>
    );
};

export const CardHeader = ({ children, className = '', action }: CardHeaderProps) => (
    <div className={`flex items-center justify-between mb-4 ${className}`}>
        <div>{children}</div>
        {action && <div>{action}</div>}
    </div>
);

export const CardTitle = ({ children, className = '' }: CardTitleProps) => (
    <h3 className={`text-lg font-semibold text-gray-900 dark:text-white ${className}`}>
        {children}
    </h3>
);

export const CardContent = ({ children, className = '' }: CardContentProps) => (
    <div className={className}>{children}</div>
);

export const CardFooter = ({ children, className = '' }: CardFooterProps) => (
    <div className={`mt-4 pt-4 border-t border-gray-200 dark:border-gray-700 ${className}`}>
        {children}
    </div>
);