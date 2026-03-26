import { NavLink } from 'react-router-dom';
import { 
    LayoutDashboard, 
    BookOpen, 
    Users, 
    ClipboardList, 
    FileText,
    Settings,
    GraduationCap,
    FolderKanban
} from 'lucide-react';
import { cn } from '../../utils/cn';
import { useAuth } from '../../context';

interface NavItem {
    label: string;
    href: string;
    icon: React.ReactNode;
    roles?: string[];
}

const navItems: NavItem[] = [
    { 
        label: 'Dashboard', 
        href: '/dashboard', 
        icon: <LayoutDashboard className="w-5 h-5" /> 
    },
    { 
        label: 'Kolegiji', 
        href: '/courses', 
        icon: <BookOpen className="w-5 h-5" />,
        roles: ['TEACHER', 'ADMIN']
    },
    { 
        label: 'Zadaci', 
        href: '/tasks', 
        icon: <ClipboardList className="w-5 h-5" /> 
    },
    { 
        label: 'Predaje', 
        href: '/submissions', 
        icon: <FileText className="w-5 h-5" /> 
    },
    { 
        label: 'Studenti', 
        href: '/students', 
        icon: <GraduationCap className="w-5 h-5" />,
        roles: ['TEACHER', 'ADMIN']
    },
    { 
        label: 'Razredi', 
        href: '/classes', 
        icon: <FolderKanban className="w-5 h-5" />,
        roles: ['TEACHER', 'ADMIN']
    },
    { 
        label: 'Korisnici', 
        href: '/users', 
        icon: <Users className="w-5 h-5" />,
        roles: ['ADMIN']
    },
    { 
        label: 'Postavke', 
        href: '/settings', 
        icon: <Settings className="w-5 h-5" /> 
    },
];

export function Sidebar() {
    const { user } = useAuth();

    const filteredItems = navItems.filter(item => {
        if (!item.roles) return true;
        return user && item.roles.includes(user.role);
    });

    return (
        <aside className="fixed left-0 top-0 h-screen w-64 bg-white dark:bg-zinc-900 border-r border-zinc-200 dark:border-zinc-800 z-30">
            <div className="flex flex-col h-full">
                {/* Logo */}
                <div className="h-16 flex items-center px-6 border-b border-zinc-100 dark:border-zinc-800">
                    <span className="text-xl font-bold text-zinc-900 dark:text-white">
                        Adaptive<span className="text-zinc-400">Learn</span>
                    </span>
                </div>

                {/* Navigation */}
                <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
                    {filteredItems.map((item) => (
                        <NavLink
                            key={item.href}
                            to={item.href}
                            className={({ isActive }) =>
                                cn(
                                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                                    isActive
                                        ? 'bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-white'
                                        : 'text-zinc-600 dark:text-zinc-400 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 hover:text-zinc-900 dark:hover:text-white'
                                )
                            }
                        >
                            {item.icon}
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                {/* User info */}
                {user && (
                    <div className="p-4 border-t border-zinc-100 dark:border-zinc-800">
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-zinc-200 dark:bg-zinc-700 flex items-center justify-center">
                                <span className="text-sm font-medium text-zinc-600 dark:text-zinc-300">
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
