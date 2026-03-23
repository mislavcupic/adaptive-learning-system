// ============================================
// ALERT - Alert/Notification komponenta
// ============================================

import { AlertCircle, CheckCircle, Info, XCircle, X } from 'lucide-react';

type AlertVariant = 'info' | 'success' | 'warning' | 'error';

interface AlertProps {
    variant?: AlertVariant;
    title?: string;
    children: React.ReactNode;
    onClose?: () => void;
    className?: string;
}

export const Alert = ({
                          variant = 'info',
                          title,
                          children,
                          onClose,
                          className = '',
                      }: AlertProps) => {
    const variantStyles: Record<AlertVariant, { bg: string; border: string; icon: React.ReactNode }> = {
        info: {
            bg: 'bg-blue-50 dark:bg-blue-900/20',
            border: 'border-blue-200 dark:border-blue-800',
            icon: <Info className="w-5 h-5 text-blue-500" />,
        },
        success: {
            bg: 'bg-green-50 dark:bg-green-900/20',
            border: 'border-green-200 dark:border-green-800',
            icon: <CheckCircle className="w-5 h-5 text-green-500" />,
        },
        warning: {
            bg: 'bg-amber-50 dark:bg-amber-900/20',
            border: 'border-amber-200 dark:border-amber-800',
            icon: <AlertCircle className="w-5 h-5 text-amber-500" />,
        },
        error: {
            bg: 'bg-red-50 dark:bg-red-900/20',
            border: 'border-red-200 dark:border-red-800',
            icon: <XCircle className="w-5 h-5 text-red-500" />,
        },
    };

    const { bg, border, icon } = variantStyles[variant];

    return (
        <div
            className={`
        ${bg} ${border} border rounded-lg p-4
        ${className}
      `}
            role="alert"
        >
            <div className="flex gap-3">
                <div className="flex-shrink-0">{icon}</div>
                <div className="flex-1">
                    {title && (
                        <h4 className="font-medium text-gray-900 dark:text-white mb-1">
                            {title}
                        </h4>
                    )}
                    <div className="text-sm text-gray-700 dark:text-gray-300">{children}</div>
                </div>
                {onClose && (
                    <button
                        onClick={onClose}
                        className="flex-shrink-0 p-1 rounded hover:bg-black/5 dark:hover:bg-white/5"
                    >
                        <X className="w-4 h-4 text-gray-500" />
                    </button>
                )}
            </div>
        </div>
    );
};