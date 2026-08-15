import { useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { authService } from '../services';

export function OAuth2CallbackPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const handled = useRef(false);

    useEffect(() => {
        if (handled.current) return;
        handled.current = true;

        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');

        if (!accessToken || !refreshToken) {
            navigate('/login?error=' + encodeURIComponent('Prijava nije uspjela.'), { replace: true });
            return;
        }

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);

        // Tokeni su spremljeni, ali podaci o korisniku jos nisu -
        // dohvacamo ih prije nego pustimo korisnika u aplikaciju.
        authService.getCurrentUser()
            .then(user => {
                localStorage.setItem('user', JSON.stringify(user));
                window.location.replace('/dashboard');
            })
            .catch(() => {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                navigate('/login?error=' + encodeURIComponent('Dohvaćanje profila nije uspjelo.'), { replace: true });
            });
    }, [searchParams, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
            <div className="text-center">
                <Loader2 className="w-10 h-10 mx-auto text-zinc-400 animate-spin" />
                <p className="mt-4 text-zinc-600 dark:text-zinc-400">Prijavljujemo vas…</p>
            </div>
        </div>
    );
}