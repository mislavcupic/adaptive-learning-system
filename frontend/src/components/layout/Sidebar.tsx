

import { NavLink, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../hooks';
import { ROUTES } from '../../constants';
import {
    LayoutDashboard,
    BookOpen,
    Users,
    FileText,
    ClipboardList,
    BarChart3,
    GraduationCap,
    TrendingUp,
    X,
} from 'lucide-react';
import type { UserRole } from '../../types';

interface SidebarProps {
    isOpen: boolean;
    onClose: () => void;
}

interface NavItem {
    label: string;
    path: string;
    icon: React.ReactNode;
    roles: UserRole[];
}

export const Sidebar = ({ isOpen, onClose }: SidebarProps) => {
    const { t } = useTranslation();
    const { user } = useAuth();
    const location = useLocation();

    // Definicija navigacijskih stavki po rolama
    const navItems: NavItem[] = [
        // Student items
        {
            label: t('nav.dashboard'),
            path: ROUTES.STUDENT_DASHBOARD,
            icon: <LayoutDashboard className="w-5 h-5" />,
            roles: ['STUDENT'],
        },
        {
            label: t('nav.courses'),
            path: ROUTES.STUDENT_COURSES,
            icon: <BookOpen className="w-5 h-5" />,
            roles: ['STUDENT'],
        },
        {
            label: t('nav.progress'),
            path: ROUTES.STUDENT_PROGRESS,
            icon: <TrendingUp className="w-5 h-5" />,
            roles: ['STUDENT'],
        },

        // Teacher items
        {
            label: t('nav.dashboard'),
            path: ROUTES.TEACHER_DASHBOARD,
            icon: <LayoutDashboard className="w-5 h-5" />,
            roles: ['TEACHER'],
        },
        {
            label: t('nav.classes'),
            path: ROUTES.TEACHER_CLASSES,
            icon: <Users className="w-5 h-5" />,
            roles: ['TEACHER'],
        },
        {
            label: t('nav.courses'),
            path: ROUTES.TEACHER_COURSES,
            icon: <BookOpen className="w-5 h-5" />,
            roles: ['TEACHER'],
        },
        {
            label: t('nav.tasks'),
            path: ROUTES.TEACHER_TASKS,
            icon: <FileText className="w-5 h-5" />,
            roles: ['TEACHER'],
        },
        {
            label: t('nav.analytics'),
            path: ROUTES.TEACHER_ANALYTICS,
            icon: <BarChart3 className="w-5 h-5" />,
            roles: ['TEACHER'],
        },

        // Admin items
        {
            label: t('nav.dashboard'),
            path: ROUTES.ADMIN_DASHBOARD,
            icon: <LayoutDashboard className="w-5 h-5" />,
            roles: ['ADMIN'],
        },
        {
            label: t('users.title'),
            path: ROUTES.ADMIN_USERS,
            icon: <Users className="w-5 h-5" />,
            roles: ['ADMIN'],
        },
        {
            label: t('nav.surveys'),
            path: ROUTES.ADMIN_SURVEYS,
            icon: <ClipboardList className="w-5 h-5" />,
            roles: ['ADMIN'],
        },
    ];

    // Filtriraj stavke po roli korisnika
    const filteredNavItems = navItems.filter(
        (item) => user && item.roles.includes(user.role)
    );

    const isActive = (path: string) => {
        return location.pathname === path || location.pathname.startsWith(path + '/');
    };

    return (
        <>
            {/* Overlay za mobile */}
            {isOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-40 lg:hidden"
                    onClick={onClose}
                />
            )}

            {/* Sidebar */}
            <aside
                className={`
          fixed top-16 left-0 bottom-0 w-64 bg-white dark:bg-gray-800 
          border-r border-gray-200 dark:border-gray-700 z-40
          transform transition-transform duration-200 ease-in-out
          lg:translate-x-0
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
            >
                {/* Mobile close button */}
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 lg:hidden"
                >
                    <X className="w-5 h-5 text-gray-500" />
                </button>

                {/* Navigation */}
                <nav className="p-4 space-y-1 mt-8 lg:mt-0">
                    {filteredNavItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            onClick={onClose}
                            className={`
                flex items-center gap-3 px-4 py-3 rounded-lg transition-colors
                ${
                                isActive(item.path)
                                    ? 'bg-indigo-50 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-300'
                                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                            }
              `}
                        >
                            {item.icon}
                            <span className="font-medium">{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                {/* Bottom section */}
                <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200 dark:border-gray-700">
                    <div className="flex items-center gap-3 px-4 py-2 text-sm text-gray-500 dark:text-gray-400">
                        <GraduationCap className="w-5 h-5" />
                        <div>
                            <p className="font-medium text-gray-900 dark:text-white">
                                {t('common.appName')}
                            </p>
                            <p className="text-xs">v1.0.0</p>
                        </div>
                    </div>
                </div>
            </aside>
        </>
    );
};