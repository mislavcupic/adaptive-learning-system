import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context';
import { useForm } from '../hooks';
import { Button, Input, Card, CardContent } from '../components/ui';
import { required, isEmail, isPassword, compose, minLength } from '../utils/validators';
import type { RegisterRequest } from '../types';
import { GoogleButton } from '../components/GoogleButton';

interface RegisterFormData extends RegisterRequest {
    confirmPassword: string;
}

export function RegisterPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { register } = useAuth();
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const form = useForm<RegisterFormData>({
        initialValues: {
            email: '',
            password: '',
            confirmPassword: '',
            firstName: '',
            lastName: '',
        },
        validators: {
            email: compose(required(t('validation.required')), isEmail) as any,
            password: compose(required(t('validation.required')), isPassword) as any,
            confirmPassword: required(t('validation.required')) as any,
            firstName: compose(required(t('validation.required')), minLength(2)) as any,
            lastName: compose(required(t('validation.required')), minLength(2)) as any,
        },
        onSubmit: async (values) => {
            if (values.password !== values.confirmPassword) {
                form.setFieldError('confirmPassword', t('validation.passwordMatch'));
                return;
            }

            setError(null);
            try {
                const { confirmPassword: _, ...registerData } = values;
                await register(registerData);
                navigate('/registration-pending');
            } catch (err) {
                setError(err instanceof Error ? err.message : t('errors.generic'));
            }
        },
    });

    return (
        <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-950 px-4 py-8">
            <div className="w-full max-w-sm">
                {/* Logo */}
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-bold text-zinc-900 dark:text-white">
                        Adaptive<span className="text-zinc-400">Learn</span>
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-2">
                        {t('auth.register')}
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

                            <div className="grid grid-cols-2 gap-3">
                                <Input
                                    label={t('auth.firstName')}
                                    name="firstName"
                                    autoComplete="given-name"
                                    value={form.values.firstName}
                                    onChange={form.handleChange}
                                    onBlur={form.handleBlur}
                                    error={form.touched.firstName ? form.errors.firstName : undefined}
                                />
                                <Input
                                    label={t('auth.lastName')}
                                    name="lastName"
                                    autoComplete="family-name"
                                    value={form.values.lastName}
                                    onChange={form.handleChange}
                                    onBlur={form.handleBlur}
                                    error={form.touched.lastName ? form.errors.lastName : undefined}
                                />
                            </div>

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
                                    autoComplete="new-password"
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

                            <Input
                                label={t('auth.confirmPassword')}
                                name="confirmPassword"
                                type={showPassword ? 'text' : 'password'}
                                autoComplete="new-password"
                                placeholder="••••••••"
                                value={form.values.confirmPassword}
                                onChange={form.handleChange}
                                onBlur={form.handleBlur}
                                error={form.touched.confirmPassword ? form.errors.confirmPassword : undefined}
                            />

                            <Button
                                type="submit"
                                fullWidth
                                isLoading={form.isSubmitting}
                            >
                                {t('auth.registerButton')}
                            </Button>
                            <div className="relative my-5">
                                <div className="absolute inset-0 flex items-center">
                                    <div className="w-full border-t border-zinc-200 dark:border-zinc-700" />
                                </div>
                                <div className="relative flex justify-center">
                                <span className="px-3 bg-white dark:bg-zinc-900 text-xs text-zinc-400 uppercase tracking-wide">
                                    ili
                                </span>
                                </div>
                            </div>

                            <GoogleButton label="Registracija putem Googlea" />
                        </form>

                        <div className="mt-6 text-center">
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">
                                {t('auth.hasAccount')}{' '}
                                <Link
                                    to="/login"
                                    className="font-medium text-zinc-900 dark:text-white hover:underline"
                                >
                                    {t('auth.loginButton')}
                                </Link>
                            </p>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}