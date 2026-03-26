import { Moon, Sun, LogOut, Menu } from 'lucide-react';
import { useAuth, useTheme } from '../../context';
import { Button } from '../ui';

interface HeaderProps {
    onMenuClick?: () => void;
}

export function Header({ onMenuClick }: HeaderProps) {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();

    const handleLogout = async () => {
        await logout();
        window.location.href = '/login';
    };

    return (
        <header className="h-16 bg-white dark:bg-zinc-900 border-b border-zinc-200 dark:border-zinc-800 px-6 flex items-center justify-between">
            <div className="flex items-center gap-4">
                {onMenuClick && (
                    <button
                        onClick={onMenuClick}
                        className="lg:hidden p-2 rounded-lg text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                    >
                        <Menu className="w-5 h-5" />
                    </button>
                )}
                <h1 className="text-lg font-medium text-zinc-900 dark:text-white">
                    {getRoleLabel(user?.role)}
                </h1>
            </div>

            <div className="flex items-center gap-2">
                <button
                    onClick={toggleTheme}
                    className="p-2 rounded-lg text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    aria-label="Toggle theme"
                >
                    {theme === 'dark' ? (
                        <Sun className="w-5 h-5" />
                    ) : (
                        <Moon className="w-5 h-5" />
                    )}
                </button>

                <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleLogout}
                    className="gap-2"
                >
                    <LogOut className="w-4 h-4" />
                    <span className="hidden sm:inline">Odjava</span>
                </Button>
            </div>
        </header>
    );
}

function getRoleLabel(role?: string): string {
    switch (role) {
        case 'ADMIN':
            return 'Administrator';
        case 'TEACHER':
            return 'Nastavnik';
        case 'STUDENT':
            return 'Student';
        default:
            return '';
    }
}
