import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';

interface LoadingProps {
    size?: 'sm' | 'md' | 'lg';
    fullScreen?: boolean;
    text?: string;
}

export const Loading = ({ size = 'md', fullScreen = false, text }: LoadingProps) => {
    const { t } = useTranslation();

    const sizeClasses = {
        sm: 'w-4 h-4',
        md: 'w-8 h-8',
        lg: 'w-12 h-12',
    };

    const spinner = (
        <div className="flex flex-col items-center justify-center gap-3">
            <Loader2 className={`${sizeClasses[size]} text-indigo-600 animate-spin`} />
            {text && (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                    {text || t('common.loading')}
                </p>
            )}
        </div>
    );

    if (fullScreen) {
        return (
            <div className="fixed inset-0 flex items-center justify-center bg-white dark:bg-gray-900">
                {spinner}
            </div>
        );
    }

    return spinner;
};