import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context';
import { useForm } from '../hooks';
import { Button, Input, Card, CardContent } from '../components/ui';
import { required, isEmail, compose } from '../utils/validators';
import type { LoginCredentials } from '../types';

export function LoginPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const form = useForm<LoginCredentials>({
        initialValues: {
            email: '',
            password: '',
        },
        validators: {
            email: compose(required(t('validation.required')), isEmail) as any,
            password: required(t('validation.required'))as any,
        },
        onSubmit: async (values) => {
            setError(null);
            try {
                await login(values);
                navigate('/dashboard');
            } catch (err) {
                setError(err instanceof Error ? err.message : t('auth.loginError'));
            }
        },
    });

    return (
        <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-950 px-4">
            <div className="w-full max-w-sm">
                {/* Logo */}
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-bold text-zinc-900 dark:text-white">
                        Adaptive<span className="text-zinc-400">Learn</span>
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-2">
                        {t('auth.welcomeMessage')}
                    </p>
                </div>

                <Card>
                    <CardContent className="pt-6">
                        <form onSubmit={form.handleSubmit} className="space-y-4">
                            {error && (
                                <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                                    <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
                                </div>
                            )}

                            <Input
                                label={t('auth.email')}
                                name="email"
                                type="email"
                                autoComplete="email"
                                placeholder="email@example.com"
                                value={form.values.email}
                                onChange={form.handleChange}
                                onBlur={form.handleBlur}
                                error={form.touched.email ? form.errors.email : undefined}
                            />

                            <div className="relative">
                                <Input
                                    label={t('auth.password')}
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    autoComplete="current-password"
                                    placeholder="••••••••"
                                    value={form.values.password}
                                    onChange={form.handleChange}
                                    onBlur={form.handleBlur}
                                    error={form.touched.password ? form.errors.password : undefined}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-[34px] text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-300"
                                >
                                    {showPassword ? (
                                        <EyeOff className="w-4 h-4" />
                                    ) : (
                                        <Eye className="w-4 h-4" />
                                    )}
                                </button>
                            </div>

                            <Button
                                type="submit"
                                fullWidth
                                isLoading={form.isSubmitting}
                            >
                                {t('auth.loginButton')}
                            </Button>
                        </form>

                        <div className="mt-6 text-center">
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">
                                {t('auth.noAccount')}{' '}
                                <Link
                                    to="/register"
                                    className="font-medium text-zinc-900 dark:text-white hover:underline"
                                >
                                    {t('auth.registerButton')}
                                </Link>
                            </p>
                        </div>
                    </CardContent>
                </Card>

                {/* Language switcher */}
                <div className="mt-6 flex justify-center gap-4">
                    <LanguageSwitch />
                </div>
            </div>
        </div>
    );
}

function LanguageSwitch() {
    const { i18n } = useTranslation();

    return (
        <div className="flex items-center gap-2 text-sm">
            <button
                onClick={() => i18n.changeLanguage('hr')}
                className={`px-2 py-1 rounded ${
                    i18n.language === 'hr'
                        ? 'bg-zinc-200 dark:bg-zinc-700 text-zinc-900 dark:text-white'
                        : 'text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300'
                }`}
            >
                HR
            </button>
            <button
                onClick={() => i18n.changeLanguage('en')}
                className={`px-2 py-1 rounded ${
                    i18n.language === 'en'
                        ? 'bg-zinc-200 dark:bg-zinc-700 text-zinc-900 dark:text-white'
                        : 'text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300'
                }`}
            >
                EN
            </button>
        </div>
    );
}
