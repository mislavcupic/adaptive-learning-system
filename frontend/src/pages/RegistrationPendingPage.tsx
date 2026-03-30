import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Clock, Mail, GraduationCap } from 'lucide-react';
import { Button } from '../components/ui';

export function RegistrationPendingPage() {
    const { t } = useTranslation();

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center p-4">
            <div className="max-w-md w-full text-center space-y-6">
                {/* Ikona */}
                <div className="mx-auto w-16 h-16 bg-amber-100 dark:bg-amber-900/30 rounded-full flex items-center justify-center">
                    <Clock className="w-8 h-8 text-amber-600 dark:text-amber-400" />
                </div>

                {/* Naslov */}
                <div className="space-y-2">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('registration.pending.title')}
                    </h1>
                    <p className="text-zinc-600 dark:text-zinc-400">
                        {t('registration.pending.subtitle')}
                    </p>
                </div>

                {/* Info kartica */}
                <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-xl p-6 space-y-4 text-left">
                    <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                        <GraduationCap className="w-5 h-5" />
                        {t('registration.pending.whatNext')}
                    </h2>

                    <ul className="space-y-3 text-sm text-zinc-600 dark:text-zinc-400">
                        <li className="flex gap-3">
                            <span className="flex-shrink-0 w-6 h-6 bg-zinc-100 dark:bg-zinc-800 rounded-full flex items-center justify-center text-xs font-medium">1</span>
                            <span>{t('registration.pending.step1')}</span>
                        </li>
                        <li className="flex gap-3">
                            <span className="flex-shrink-0 w-6 h-6 bg-zinc-100 dark:bg-zinc-800 rounded-full flex items-center justify-center text-xs font-medium">2</span>
                            <span>{t('registration.pending.step2')}</span>
                        </li>
                        <li className="flex gap-3">
                            <span className="flex-shrink-0 w-6 h-6 bg-zinc-100 dark:bg-zinc-800 rounded-full flex items-center justify-center text-xs font-medium">3</span>
                            <span>{t('registration.pending.step3')}</span>
                        </li>
                    </ul>
                </div>

                {/* Email info */}
                <div className="flex items-center justify-center gap-2 text-sm text-zinc-500 dark:text-zinc-500">
                    <Mail className="w-4 h-4" />
                    <span>{t('registration.pending.checkEmail')}</span>
                </div>

                {/* Povratak */}
                <Link to="/login">
                    <Button variant="ghost" className="mt-4">
                        ← {t('registration.pending.backToLogin')}
                    </Button>
                </Link>
            </div>
        </div>
    );
}