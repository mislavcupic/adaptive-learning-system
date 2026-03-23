//import { useTranslation } from 'react-i18next';
import { useFont } from '../../context/FontContext';

const fontOptions = [
    { id: 'default', label: 'Modern (Inter)', icon: 'Aa' },
    { id: 'typewriter', label: 'Typewriter', icon: 'Tt' },
    { id: 'dyslexic', label: 'Dyslexia-Friendly', icon: 'Dd' },
    { id: 'lexend', label: 'Lexend', icon: 'Ll' },
] as const;

export const FontSelector = () => {
 //   const { t } = useTranslation();
    const { fontMode, setFontMode } = useFont();

    return (
        <div className="flex items-center gap-1 p-1 bg-surface-800/50 rounded-lg border border-surface-700">
            {fontOptions.map((option) => (
                <button
                    key={option.id}
                    onClick={() => setFontMode(option.id)}
                    className={`
            px-3 py-1.5 rounded-md text-sm font-medium transition-all
            ${fontMode === option.id
                        ? 'bg-primary-600 text-white shadow-glow'
                        : 'text-surface-400 hover:text-white hover:bg-surface-700'
                    }
          `}
                    title={option.label}
                >
                    {option.icon}
                </button>
            ))}
        </div>
    );
};