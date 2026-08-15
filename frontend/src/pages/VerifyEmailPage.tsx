import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2, Mail, ArrowRight } from 'lucide-react';
import { authService } from '../services';
import { Card, CardContent, Button } from '../components/ui';

type Status = 'loading' | 'success' | 'error';

export function VerifyEmailPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [status, setStatus] = useState<Status>('loading');
    const [message, setMessage] = useState('');
    const [email, setEmail] = useState('');
    const [resending, setResending] = useState(false);
    const [resendMessage, setResendMessage] = useState('');

    // StrictMode u razvoju dvaput pokrece efekt; token je jednokratan
    // pa bi drugi poziv javio gresku bez ove zastite.
    const attempted = useRef(false);

    useEffect(() => {
        if (attempted.current) return;
        attempted.current = true;

        if (!token) {
            setStatus('error');
            setMessage('Poveznica ne sadrži token za potvrdu.');
            return;
        }

        authService.verifyEmail(token)
            .then(msg => {
                setStatus('success');
                setMessage(msg);
            })
            .catch(err => {
                setStatus('error');
                setMessage(err instanceof Error ? err.message : 'Potvrda nije uspjela.');
            });
    }, [token]);

    const handleResend = async () => {
        if (!email.trim()) return;
        setResending(true);
        setResendMessage('');
        try {
            const msg = await authService.resendVerification(email.trim());
            setResendMessage(msg);
        } catch {
            setResendMessage('Slanje nije uspjelo. Pokušajte ponovno.');
        } finally {
            setResending(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900 px-4">
            <div className="w-full max-w-md">
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        AdaptiveLearn
                    </h1>
                </div>

                <Card>
                    <CardContent className="py-10 px-8">
                        {status === 'loading' && (
                            <div className="text-center">
                                <Loader2 className="w-12 h-12 mx-auto text-zinc-400 animate-spin" />
                                <p className="mt-4 text-zinc-600 dark:text-zinc-400">
                                    Potvrđujemo vašu e-mail adresu…
                                </p>
                            </div>
                        )}

                        {status === 'success' && (
                            <div className="text-center">
                                <div className="w-16 h-16 mx-auto rounded-full bg-emerald-50 dark:bg-emerald-900/30 flex items-center justify-center">
                                    <CheckCircle2 className="w-9 h-9 text-emerald-600" />
                                </div>
                                <h2 className="mt-5 text-xl font-semibold text-zinc-900 dark:text-white">
                                    E-mail adresa je potvrđena
                                </h2>
                                <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-400 leading-relaxed">
                                    {message}
                                </p>

                                <div className="mt-6 p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 text-left">
                                    <p className="text-sm text-zinc-700 dark:text-zinc-300 leading-relaxed">
                                        <strong>Što slijedi?</strong> Vaš nastavnik sada vidi vašu prijavu
                                        i dodijelit će vas u razred. O aktivaciji računa bit ćete
                                        obaviješteni e-mailom.
                                    </p>
                                </div>

                                <Button
                                    onClick={() => navigate('/login')}
                                    className="mt-6 w-full gap-2"
                                >
                                    Idi na prijavu
                                    <ArrowRight className="w-4 h-4" />
                                </Button>
                            </div>
                        )}

                        {status === 'error' && (
                            <div className="text-center">
                                <div className="w-16 h-16 mx-auto rounded-full bg-red-50 dark:bg-red-900/30 flex items-center justify-center">
                                    <XCircle className="w-9 h-9 text-red-500" />
                                </div>
                                <h2 className="mt-5 text-xl font-semibold text-zinc-900 dark:text-white">
                                    Potvrda nije uspjela
                                </h2>
                                <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-400 leading-relaxed">
                                    {message}
                                </p>

                                <div className="mt-6 pt-6 border-t border-zinc-100 dark:border-zinc-800 text-left">
                                    <p className="text-sm font-medium text-zinc-900 dark:text-white mb-1">
                                        Zatražite novu poveznicu
                                    </p>
                                    <p className="text-xs text-zinc-500 mb-3">
                                        Upišite adresu kojom ste se registrirali.
                                    </p>

                                    <div className="flex gap-2">
                                        <div className="relative flex-1">
                                            <Mail className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
                                            <input
                                                type="email"
                                                value={email}
                                                onChange={(e) => setEmail(e.target.value)}
                                                placeholder="vas.mail@primjer.hr"
                                                className="w-full pl-9 pr-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                            />
                                        </div>
                                        <Button
                                            onClick={handleResend}
                                            disabled={!email.trim() || resending}
                                            isLoading={resending}
                                        >
                                            Pošalji
                                        </Button>
                                    </div>

                                    {resendMessage && (
                                        <p className="mt-3 text-sm text-emerald-600 dark:text-emerald-400">
                                            {resendMessage}
                                        </p>
                                    )}
                                </div>
                            </div>
                        )}
                    </CardContent>
                </Card>

                <p className="mt-6 text-center text-sm text-zinc-500">
                    <Link to="/login" className="hover:text-zinc-900 dark:hover:text-white">
                        Povratak na prijavu
                    </Link>
                </p>
            </div>
        </div>
    );
}