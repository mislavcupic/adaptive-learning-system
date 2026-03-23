import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff, LogIn, Code2, Sparkles, Zap, BookOpen } from 'lucide-react';
import { authService } from '../../services';
import type { UserRole } from '../../types';
import { ROUTES } from '../../constants';
import { useLanguage } from '../../hooks';
import { useFont } from '../../context/FontContext';

const fontOptions = [
    { id: 'default', label: 'Modern', preview: 'Aa' },
    { id: 'typewriter', label: 'Typewriter', preview: 'Tt' },
    { id: 'dyslexic', label: 'Dyslexia', preview: 'Dd' },
    { id: 'lexend', label: 'Lexend', preview: 'Ll' },
] as const;

export const Login = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { currentLanguage, toggleLanguage } = useLanguage();
    const { fontMode, setFontMode } = useFont();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [typedText, setTypedText] = useState('');

    // Typewriter effect
    const fullText = 'Učenje programiranja prilagođeno tebi.';
    useEffect(() => {
        let index = 0;
        const timer = setInterval(() => {
            if (index <= fullText.length) {
                setTypedText(fullText.slice(0, index));
                index++;
            } else {
                clearInterval(timer);
            }
        }, 50);
        return () => clearInterval(timer);
    }, []);

    const handleSubmit = async (e: React.SyntheticEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const user = await authService.login({ email, password });
            redirectByRole(user.role);
        } catch {
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
        <div className="min-h-screen bg-[#0a0a0f] flex overflow-hidden">

            {/* Left side - Branding */}
            <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
                {/* Animated gradient background */}
                <div className="absolute inset-0 bg-gradient-to-br from-violet-600/20 via-transparent to-cyan-600/20" />

                {/* Grid pattern */}
                <div
                    className="absolute inset-0 opacity-20"
                    style={{
                        backgroundImage: `
              linear-gradient(rgba(139, 92, 246, 0.1) 1px, transparent 1px),
              linear-gradient(90deg, rgba(139, 92, 246, 0.1) 1px, transparent 1px)
            `,
                        backgroundSize: '50px 50px'
                    }}
                />

                {/* Floating code snippets */}
                <div className="absolute top-20 left-10 bg-[#1a1a2e]/80 backdrop-blur-xl rounded-xl p-4 border border-violet-500/20 shadow-2xl transform -rotate-3 hover:rotate-0 transition-transform duration-500">
          <pre className="text-sm font-mono">
            <span className="text-violet-400">int</span>{" "}
              <span className="text-cyan-400">main</span>
            <span className="text-gray-400">()</span> {"{"}
              {"\n"}  <span className="text-violet-400">printf</span>
            <span className="text-gray-400">(</span>
            <span className="text-emerald-400">"Hello!"</span>
            <span className="text-gray-400">);</span>
              {"\n"}{"}"}
          </pre>
                </div>

                <div className="absolute top-40 right-20 bg-[#1a1a2e]/80 backdrop-blur-xl rounded-xl p-4 border border-cyan-500/20 shadow-2xl transform rotate-2 hover:rotate-0 transition-transform duration-500">
          <pre className="text-sm font-mono">
            <span className="text-violet-400">for</span>{" "}
              <span className="text-gray-400">(</span>
            <span className="text-cyan-400">i</span>
            <span className="text-gray-400">=</span>
            <span className="text-amber-400">0</span>
            <span className="text-gray-400">;</span>{" "}
              <span className="text-cyan-400">i</span>
            <span className="text-gray-400">&lt;</span>
            <span className="text-amber-400">10</span>
            <span className="text-gray-400">;</span>{" "}
              <span className="text-cyan-400">i</span>
            <span className="text-violet-400">++</span>
            <span className="text-gray-400">)</span>
          </pre>
                </div>

                <div className="absolute bottom-32 left-20 bg-[#1a1a2e]/80 backdrop-blur-xl rounded-xl p-4 border border-emerald-500/20 shadow-2xl transform rotate-1 hover:rotate-0 transition-transform duration-500">
          <pre className="text-sm font-mono">
            <span className="text-violet-400">while</span>{" "}
              <span className="text-gray-400">(</span>
            <span className="text-cyan-400">learning</span>
            <span className="text-gray-400">)</span> {"{"}
              {"\n"}  <span className="text-emerald-400">improve</span>
            <span className="text-gray-400">();</span>
              {"\n"}{"}"}
          </pre>
                </div>

                {/* Glowing orbs */}
                <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-violet-600/30 rounded-full blur-[100px] animate-pulse" />
                <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-cyan-600/30 rounded-full blur-[100px] animate-pulse" style={{ animationDelay: '1s' }} />

                {/* Content */}
                <div className="relative z-10 flex flex-col justify-center px-16">
                    <div className="flex items-center gap-3 mb-8">
                        <div className="w-14 h-14 bg-gradient-to-br from-violet-500 to-cyan-500 rounded-2xl flex items-center justify-center shadow-lg shadow-violet-500/25">
                            <Code2 className="w-8 h-8 text-white" />
                        </div>
                        <span className="text-2xl font-bold text-white tracking-tight">
              {t('common.appName')}
            </span>
                    </div>

                    <h1 className="text-5xl font-bold text-white leading-tight mb-6">
                        Nauči programirati
                        <br />
                        <span className="bg-gradient-to-r from-violet-400 to-cyan-400 bg-clip-text text-transparent">
              na svoj način
            </span>
                    </h1>

                    <p className="text-xl text-gray-400 mb-8 h-8 font-mono">
                        {typedText}
                        <span className="animate-pulse text-violet-400">|</span>
                    </p>

                    {/* Features */}
                    <div className="space-y-4">
                        <Feature icon={<Sparkles className="w-5 h-5" />} text="AI personalizirani feedback" />
                        <Feature icon={<Zap className="w-5 h-5" />} text="Praćenje napretka u stvarnom vremenu" />
                        <Feature icon={<BookOpen className="w-5 h-5" />} text="Prilagođeno učeničkim potrebama" />
                    </div>
                </div>
            </div>

            {/* Right side - Login form */}
            <div className="w-full lg:w-1/2 flex flex-col">

                {/* Top bar */}
                <div className="flex justify-end items-center gap-3 p-6">
                    {/* Font selector */}
                    <div className="flex items-center bg-[#1a1a2e] rounded-xl border border-white/5 p-1">
                        {fontOptions.map((option) => (
                            <button
                                key={option.id}
                                onClick={() => setFontMode(option.id)}
                                className={`
                  px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200
                  ${fontMode === option.id
                                    ? 'bg-gradient-to-r from-violet-600 to-violet-500 text-white shadow-lg shadow-violet-500/25'
                                    : 'text-gray-500 hover:text-white'
                                }
                `}
                                title={option.label}
                            >
                                {option.preview}
                            </button>
                        ))}
                    </div>

                    {/* Language toggle */}
                    <button
                        onClick={toggleLanguage}
                        className="px-4 py-2 rounded-xl bg-[#1a1a2e] border border-white/5 text-gray-400 hover:text-white hover:border-violet-500/50 transition-all duration-200 text-sm font-medium"
                    >
                        {currentLanguage === 'hr' ? '🇭🇷 HR' : '🇬🇧 EN'}
                    </button>
                </div>

                {/* Login form */}
                <div className="flex-1 flex items-center justify-center px-6 lg:px-16">
                    <div className="w-full max-w-md">

                        {/* Mobile logo */}
                        <div className="lg:hidden text-center mb-10">
                            <div className="w-16 h-16 bg-gradient-to-br from-violet-500 to-cyan-500 rounded-2xl flex items-center justify-center shadow-lg shadow-violet-500/25 mx-auto mb-4">
                                <Code2 className="w-9 h-9 text-white" />
                            </div>
                            <h1 className="text-2xl font-bold text-white">{t('common.appName')}</h1>
                        </div>

                        <div className="mb-10">
                            <h2 className="text-3xl font-bold text-white mb-2">
                                Dobrodošli natrag
                            </h2>
                            <p className="text-gray-500">
                                Prijavite se za nastavak učenja
                            </p>
                        </div>

                        {error && (
                            <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm flex items-center gap-3">
                                <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
                                {error}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} className="space-y-6">
                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-2">
                                    {t('auth.email')}
                                </label>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="ime@skola.hr"
                                    required
                                    autoComplete="email"
                                    className="w-full px-4 py-4 rounded-xl bg-[#1a1a2e] border border-white/5 text-white placeholder-gray-600 focus:outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10 transition-all duration-200"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-2">
                                    {t('auth.password')}
                                </label>
                                <div className="relative">
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        placeholder="••••••••••••"
                                        required
                                        autoComplete="current-password"
                                        className="w-full px-4 py-4 pr-12 rounded-xl bg-[#1a1a2e] border border-white/5 text-white placeholder-gray-600 focus:outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10 transition-all duration-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-500 hover:text-white transition-colors"
                                    >
                                        {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full py-4 px-6 rounded-xl bg-gradient-to-r from-violet-600 to-violet-500 hover:from-violet-500 hover:to-violet-400 text-white font-semibold text-lg shadow-xl shadow-violet-500/25 hover:shadow-violet-500/40 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 transition-all duration-200 flex items-center justify-center gap-3"
                            >
                                {isLoading ? (
                                    <div className="w-6 h-6 border-3 border-white/30 border-t-white rounded-full animate-spin" />
                                ) : (
                                    <>
                                        <LogIn className="w-5 h-5" />
                                        {t('auth.loginButton')}
                                    </>
                                )}
                            </button>
                        </form>

                        {/* Demo accounts */}
                        <div className="mt-10 pt-8 border-t border-white/5">
                            <p className="text-sm text-gray-600 text-center mb-4">Demo računi za testiranje:</p>
                            <div className="grid grid-cols-3 gap-2">
                                <DemoAccount role="Student" email="student@test.hr" />
                                <DemoAccount role="Nastavnik" email="teacher@test.hr" />
                                <DemoAccount role="Admin" email="admin@test.hr" />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="p-6 text-center">
                    <p className="text-gray-600 text-sm">
                        v1.0.0 • Algebra University College © 2026
                    </p>
                </div>
            </div>
        </div>
    );
};

// Feature component
const Feature = ({ icon, text }: { icon: React.ReactNode; text: string }) => (
    <div className="flex items-center gap-4 text-gray-300">
        <div className="w-10 h-10 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center text-violet-400">
            {icon}
        </div>
        <span>{text}</span>
    </div>
);

// Demo account component
const DemoAccount = ({ role, email }: { role: string; email: string }) => (
    <button
        className="p-3 rounded-xl bg-[#1a1a2e] border border-white/5 hover:border-violet-500/30 transition-all duration-200 text-left group"
        onClick={() => {
            navigator.clipboard.writeText(email);
        }}
        title={`Kopiraj: ${email}`}
    >
        <p className="text-xs text-gray-500 group-hover:text-gray-400">{role}</p>
        <p className="text-xs text-violet-400 truncate">{email}</p>
    </button>
);