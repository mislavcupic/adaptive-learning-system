import { useTranslation } from 'react-i18next';
import { Moon, Sun, Type, Languages, Eye, Zap } from 'lucide-react';
import { useTheme, useAccessibility } from '../context';
import type { FontFamily, FontSize } from '../context';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui';

export function SettingsPage() {
    const { t } = useTranslation();

    return (
        <div className="max-w-2xl space-y-6 animate-fade-in">
            <div>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    {t('settings.title')}
                </h1>
                <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                    Prilagodite aplikaciju svojim potrebama
                </p>
            </div>

            {/* Appearance */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Sun className="w-5 h-5" />
                        {t('settings.appearance')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                    <ThemeSelector />
                    <LanguageSelector />
                </CardContent>
            </Card>

            {/* Accessibility */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Eye className="w-5 h-5" />
                        {t('settings.accessibility')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                    <FontSelector />
                    <FontSizeSelector />
                    <ContrastToggle />
                    <MotionToggle />
                </CardContent>
            </Card>
        </div>
    );
}

function ThemeSelector() {
    const { t } = useTranslation();
    const { theme, setTheme } = useTheme();

    const themes = [
        { value: 'light', label: t('settings.lightTheme'), icon: Sun },
        { value: 'dark', label: t('settings.darkTheme'), icon: Moon },
    ];

    return (
        <div>
            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-3">
                {t('settings.theme')}
            </label>
            <div className="flex gap-3">
                {themes.map(({ value, label, icon: Icon }) => (
                    <button
                        key={value}
                        onClick={() => setTheme(value as 'light' | 'dark')}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg border transition-colors ${
                            theme === value
                                ? 'border-zinc-900 dark:border-white bg-zinc-900 dark:bg-white text-white dark:text-zinc-900'
                                : 'border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-400 hover:border-zinc-300 dark:hover:border-zinc-600'
                        }`}
                    >
                        <Icon className="w-4 h-4" />
                        {label}
                    </button>
                ))}
            </div>
        </div>
    );
}

function LanguageSelector() {
    const { t, i18n } = useTranslation();

    const languages = [
        { value: 'hr', label: t('settings.croatian'), flag: '🇭🇷' },
        { value: 'en', label: t('settings.english'), flag: '🇬🇧' },
    ];

    return (
        <div>
            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-3">
                <Languages className="w-4 h-4 inline mr-2" />
                {t('settings.language')}
            </label>
            <div className="flex gap-3">
                {languages.map(({ value, label, flag }) => (
                    <button
                        key={value}
                        onClick={() => i18n.changeLanguage(value)}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg border transition-colors ${
                            i18n.language === value
                                ? 'border-zinc-900 dark:border-white bg-zinc-900 dark:bg-white text-white dark:text-zinc-900'
                                : 'border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-400 hover:border-zinc-300 dark:hover:border-zinc-600'
                        }`}
                    >
                        <span>{flag}</span>
                        {label}
                    </button>
                ))}
            </div>
        </div>
    );
}

function FontSelector() {
    const { t } = useTranslation();
    const { fontFamily, setFontFamily } = useAccessibility();

    const fonts: { value: FontFamily; label: string; sample: string }[] = [
        { value: 'default', label: t('settings.fontDefault'), sample: 'Aa Bb Cc' },
        { value: 'dyslexic', label: t('settings.fontDyslexia'), sample: 'Aa Bb Cc' },
        { value: 'mono', label: t('settings.fontMono'), sample: 'Aa Bb Cc' },
        { value: 'serif', label: t('settings.fontSerif'), sample: 'Aa Bb Cc' },
    ];

    const fontClasses: Record<FontFamily, string> = {
        default: 'font-sans',
        dyslexic: 'font-dyslexic',
        mono: 'font-mono',
        serif: 'font-serif',
    };

    return (
        <div>
            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-3">
                <Type className="w-4 h-4 inline mr-2" />
                {t('settings.font')}
            </label>
            <div className="grid grid-cols-2 gap-3">
                {fonts.map(({ value, label, sample }) => (
                    <button
                        key={value}
                        onClick={() => setFontFamily(value)}
                        className={`p-4 rounded-lg border text-left transition-colors ${
                            fontFamily === value
                                ? 'border-zinc-900 dark:border-white bg-zinc-50 dark:bg-zinc-800'
                                : 'border-zinc-200 dark:border-zinc-700 hover:border-zinc-300 dark:hover:border-zinc-600'
                        }`}
                    >
                        <p className={`text-lg ${fontClasses[value]} text-zinc-900 dark:text-white`}>
                            {sample}
                        </p>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {label}
                        </p>
                    </button>
                ))}
            </div>
            {fontFamily === 'dyslexic' && (
                <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-3">
                    OpenDyslexic font je dizajniran za osobe s disleksijom. Svako slovo ima težu bazu
                    kako bi se smanjila rotacija i zamjena slova.
                </p>
            )}
        </div>
    );
}

function FontSizeSelector() {
    const { t } = useTranslation();
    const { fontSize, setFontSize } = useAccessibility();

    const sizes: { value: FontSize; label: string; preview: string }[] = [
        { value: 'small', label: t('settings.fontSizeSmall'), preview: 'A' },
        { value: 'normal', label: t('settings.fontSizeNormal'), preview: 'A' },
        { value: 'large', label: t('settings.fontSizeLarge'), preview: 'A' },
        { value: 'xlarge', label: t('settings.fontSizeXLarge'), preview: 'A' },
    ];

    const sizeClasses: Record<FontSize, string> = {
        small: 'text-sm',
        normal: 'text-base',
        large: 'text-lg',
        xlarge: 'text-xl',
    };

    return (
        <div>
            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-3">
                {t('settings.fontSize')}
            </label>
            <div className="flex gap-3">
                {sizes.map(({ value, label, preview }) => (
                    <button
                        key={value}
                        onClick={() => setFontSize(value)}
                        className={`flex-1 py-3 rounded-lg border transition-colors ${
                            fontSize === value
                                ? 'border-zinc-900 dark:border-white bg-zinc-900 dark:bg-white text-white dark:text-zinc-900'
                                : 'border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-400 hover:border-zinc-300 dark:hover:border-zinc-600'
                        }`}
                    >
                        <span className={sizeClasses[value]}>{preview}</span>
                        <p className="text-xs mt-1 opacity-70">{label}</p>
                    </button>
                ))}
            </div>
        </div>
    );
}

function ContrastToggle() {
    const { t } = useTranslation();
    const { highContrast, setHighContrast } = useAccessibility();

    return (
        <div className="flex items-center justify-between">
            <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300">
                    {t('settings.highContrast')}
                </label>
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    Povećava kontrast za bolju čitljivost
                </p>
            </div>
            <Toggle checked={highContrast} onChange={setHighContrast} />
        </div>
    );
}

function MotionToggle() {
    const { t } = useTranslation();
    const { reducedMotion, setReducedMotion } = useAccessibility();

    return (
        <div className="flex items-center justify-between">
            <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300">
                    <Zap className="w-4 h-4 inline mr-2" />
                    {t('settings.reducedMotion')}
                </label>
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    Smanjuje animacije u aplikaciji
                </p>
            </div>
            <Toggle checked={reducedMotion} onChange={setReducedMotion} />
        </div>
    );
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: (value: boolean) => void }) {
    return (
        <button
            role="switch"
            aria-checked={checked}
            onClick={() => onChange(!checked)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                checked ? 'bg-zinc-900 dark:bg-white' : 'bg-zinc-200 dark:bg-zinc-700'
            }`}
        >
            <span
                className={`inline-block h-4 w-4 transform rounded-full transition-transform ${
                    checked 
                        ? 'translate-x-6 bg-white dark:bg-zinc-900' 
                        : 'translate-x-1 bg-white dark:bg-zinc-400'
                }`}
            />
        </button>
    );
}
