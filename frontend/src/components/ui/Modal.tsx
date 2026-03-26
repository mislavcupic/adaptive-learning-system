import { Fragment, ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '../../utils/cn';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    description?: string;
    children: ReactNode;
    size?: 'sm' | 'md' | 'lg' | 'xl';
}

const sizes = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
};

export function Modal({ isOpen, onClose, title, description, children, size = 'md' }: ModalProps) {
    if (!isOpen) return null;

    return (
        <Fragment>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                <div
                    className={cn(
                        'w-full bg-white dark:bg-zinc-900 rounded-xl shadow-xl',
                        'border border-zinc-200 dark:border-zinc-800',
                        'transform transition-all',
                        sizes[size]
                    )}
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    {(title || description) && (
                        <div className="px-6 py-4 border-b border-zinc-100 dark:border-zinc-800">
                            <div className="flex items-start justify-between">
                                <div>
                                    {title && (
                                        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
                                            {title}
                                        </h2>
                                    )}
                                    {description && (
                                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                                            {description}
                                        </p>
                                    )}
                                </div>
                                <button
                                    onClick={onClose}
                                    className="p-1 rounded-lg text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                                >
                                    <X className="w-5 h-5" />
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Content */}
                    <div className="px-6 py-4">{children}</div>
                </div>
            </div>
        </Fragment>
    );
}

export function ModalFooter({ children, className }: { children: ReactNode; className?: string }) {
    return (
        <div
            className={cn(
                'flex items-center justify-end gap-3 pt-4 mt-4 border-t border-zinc-100 dark:border-zinc-800',
                className
            )}
        >
            {children}
        </div>
    );
}
