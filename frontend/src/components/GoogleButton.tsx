interface GoogleButtonProps {
    label?: string;
}

export function GoogleButton({ label = 'Nastavi putem Googlea' }: GoogleButtonProps) {
    const handleClick = () => {
        // Puna navigacija na backend - OAuth2 tijek ne moze kroz fetch.
        const apiBase = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
        const backendRoot = apiBase.replace(/\/api\/?$/, '');
        window.location.href = `${backendRoot}/oauth2/authorization/google`;
    };

    return (
        <button
            type="button"
            onClick={handleClick}
            className="w-full flex items-center justify-center gap-3 px-4 py-2.5 rounded-lg
                       border border-zinc-300 dark:border-zinc-600
                       bg-white dark:bg-zinc-800
                       text-sm font-medium text-zinc-700 dark:text-zinc-200
                       hover:bg-zinc-50 dark:hover:bg-zinc-700
                       transition-colors"
        >
            <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.76h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.76c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.11a6.6 6.6 0 0 1 0-4.22V7.05H2.18a11 11 0 0 0 0 9.9l3.66-2.84z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1a11 11 0 0 0-9.82 6.05l3.66 2.84c.87-2.6 3.3-4.51 6.16-4.51z"/>
            </svg>
            {label}
        </button>
    );
}