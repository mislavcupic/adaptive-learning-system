import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff, LogIn } from 'lucide-react';
import { Button, Input, Alert } from '../../components';
import { authService } from '../../services';
import type { UserRole } from '../../types';
import { ROUTES } from '../../constants';

export const Login = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e: React.SyntheticEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const user = await authService.login({ email, password });
            redirectByRole(user.role);
        } catch (err) {
            setError(t('auth.loginError'));
        } finally {
            setIsLoading(false);
        }
    };

    const redirectByRole = (role: UserRole) => {
        switch (role) {
            case 'ADMIN':
                navigate(ROUTES.ADMIN_DASHBOARD);
                break;
            case 'TEACHER':
                navigate(ROUTES.TEACHER_DASHBOARD);
                break;
            case 'STUDENT':
                navigate(ROUTES.STUDENT_DASHBOARD);
                break;
            default:
                navigate('/');
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">

                    {/* Logo */}
                    <div className="text-center mb-8">
                        <div className="w-16 h-16 bg-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
                            <span className="text-white font-bold text-3xl">A</span>
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                            {t('common.appName')}
                        </h1>
                        <p className="text-gray-500 dark:text-gray-400 mt-2">
                            {t('auth.login')}
                        </p>
                    </div>

                    {/* Error alert */}
                    {error && (
                        <Alert variant="error" className="mb-6" onClose={() => setError('')}>
                            {error}
                        </Alert>
                    )}

                    {/* Form */}
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <Input
                            label={t('auth.email')}
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="ime@skola.hr"
                            required
                            autoComplete="email"
                        />

                        <Input
                            label={t('auth.password')}
                            type={showPassword ? 'text' : 'password'}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                            autoComplete="current-password"
                            rightIcon={
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="text-gray-400 hover:text-gray-600"
                                >
                                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                </button>
                            }
                        />

                        <Button
                            type="submit"
                            fullWidth
                            isLoading={isLoading}
                            leftIcon={<LogIn className="w-5 h-5" />}
                        >
                            {isLoading ? t('auth.loggingIn') : t('auth.loginButton')}
                        </Button>
                    </form>

                </div>
            </div>
        </div>
    );
};


