import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    LayoutDashboard,
    BookOpen,
    Users,
    ClipboardList,
    ClipboardCheck,
    FileText,
    Settings,
    GraduationCap,
    FolderKanban,
    UserPlus,
    FileCheck,
    TrendingUp,
    ShieldCheck
} from 'lucide-react';
import { cn } from '../../utils/cn';
import { useAuth } from '../../context';
import { useFetch } from '../../hooks';
import { apiClient } from '../../api/client';


interface NavItem {
    label: string;
    href: string;
    icon: React.ReactNode;
    roles?: string[];
    badge?: number;
}

export function Sidebar() {
    const { t } = useTranslation();
    const { user } = useAuth();

    const { data: pendingCount } = useFetch<number>(
        async () => {
            if (!user || !['TEACHER', 'ADMIN'].includes(user.role)) return 0;
            const res = await apiClient.get('/teacher/pending-registrations/count');
            return (res as { data: number }).data;
        },
        [user?.role]
    );

    const navItems: NavItem[] = [
        {
            label: t('nav.dashboard'),
            href: '/dashboard',
            icon: <LayoutDashboard className="w-5 h-5" />
        },
        {
            label: t('nav.courses'),
            href: '/courses',
            icon: <BookOpen className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN']
        },
        {
            label: t('nav.tasks'),
            href: '/tasks',
            icon: <ClipboardList className="w-5 h-5" />
        },
        {
            label: t('nav.assessments'),
            href: '/assessments',
            icon: <FileCheck className="w-5 h-5" />
        },
        {
            label: t('nav.exams'),
            href: '/exams',
            icon: <ClipboardCheck className="w-5 h-5" />
        },

        {
            label: t('nav.submissions'),
            href: '/submissions',
            icon: <FileText className="w-5 h-5" />
        },
        {
            label: t('nav.students'),
            href: '/students',
            icon: <GraduationCap className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN']
        },
        {
            label: t('nav.classes'),
            href: '/classes',
            icon: <FolderKanban className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN']
        },
        {
            label: t('nav.pending'),
            href: '/pending-registrations',
            icon: <UserPlus className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN'],
            badge: pendingCount || 0
        },
        {
            label: t('nav.users'),
            href: '/users',
            icon: <Users className="w-5 h-5" />,
            roles: ['ADMIN']
        },
        {
            label: t('nav.research'),
            href: '/research-results',
            icon: <TrendingUp className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN']
        },
        {
            label: t('nav.audit'),
            href: '/audit-logs',
            icon: <ShieldCheck className="w-5 h-5" />,
            roles: ['TEACHER', 'ADMIN']
        },
        {
            label: t('nav.settings'),
            href: '/settings',
            icon: <Settings className="w-5 h-5" />
        },
    ];

    const filteredItems = navItems.filter(item => {
        if (!item.roles) return true;
        return user && item.roles.includes(user.role);
    });

    return (
        <aside className="fixed left-0 top-0 h-screen w-64 bg-zinc-50 dark:bg-zinc-950 border-r border-zinc-200 dark:border-zinc-800 z-30">
            <div className="flex flex-col h-full">
                <div className="h-16 flex items-center px-6">
                    <span className="text-xl font-bold tracking-tight text-zinc-900 dark:text-white">
                        Adaptive<span className="text-teal-700 dark:text-teal-300">Learn</span>
                    </span>
                </div>

                <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
                    {filteredItems.map((item) => (
                        <NavLink
                            key={item.href}
                            to={item.href}
                            className={({ isActive }) =>
                                cn(
                                    'flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-colors',
                                    isActive
                                        ? 'bg-teal-700 text-white font-semibold dark:bg-teal-300 dark:text-teal-950'
                                        : 'text-zinc-600 dark:text-zinc-400 hover:bg-white dark:hover:bg-zinc-900 hover:text-zinc-900 dark:hover:text-white'
                                )
                            }
                        >
                            {item.icon}
                            <span className="flex-1">{item.label}</span>
                            {item.badge !== undefined && item.badge > 0 && (
                                <span className="bg-red-500 text-white text-xs px-2 py-0.5 rounded-full min-w-[20px] text-center">
                                    {item.badge}
                                </span>
                            )}
                        </NavLink>
                    ))}
                </nav>

                {user && (
                    <div className="p-4 border-t border-zinc-200 dark:border-zinc-800">
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-teal-100 dark:bg-teal-900/50 flex items-center justify-center">
                                <span className="text-sm font-semibold text-teal-800 dark:text-teal-200">
                                    {user.firstName?.charAt(0)}{user.lastName?.charAt(0)}
                                </span>
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                                    {user.firstName} {user.lastName}
                                </p>
                                <p className="text-xs text-zinc-500 dark:text-zinc-400 truncate">
                                    {user.email}
                                </p>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </aside>
    );
}