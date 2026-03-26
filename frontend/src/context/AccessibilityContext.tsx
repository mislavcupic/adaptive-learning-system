import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

export type FontFamily = 'default' | 'dyslexic' | 'mono' | 'serif';
export type FontSize = 'small' | 'normal' | 'large' | 'xlarge';

interface AccessibilitySettings {
    fontFamily: FontFamily;
    fontSize: FontSize;
    highContrast: boolean;
    reducedMotion: boolean;
}

interface AccessibilityContextType extends AccessibilitySettings {
    setFontFamily: (font: FontFamily) => void;
    setFontSize: (size: FontSize) => void;
    setHighContrast: (enabled: boolean) => void;
    setReducedMotion: (enabled: boolean) => void;
    resetSettings: () => void;
}

const defaultSettings: AccessibilitySettings = {
    fontFamily: 'default',
    fontSize: 'normal',
    highContrast: false,
    reducedMotion: false,
};

const AccessibilityContext = createContext<AccessibilityContextType | undefined>(undefined);

const STORAGE_KEY = 'accessibility-settings';

// Font family CSS classes
const fontFamilyClasses: Record<FontFamily, string> = {
    default: 'font-sans',
    dyslexic: 'font-dyslexic',
    mono: 'font-mono',
    serif: 'font-serif',
};

// Font size CSS classes
const fontSizeClasses: Record<FontSize, string> = {
    small: 'text-sm',
    normal: 'text-base',
    large: 'text-lg',
    xlarge: 'text-xl',
};

export function AccessibilityProvider({ children }: { children: ReactNode }) {
    const [settings, setSettings] = useState<AccessibilitySettings>(() => {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            try {
                return { ...defaultSettings, ...JSON.parse(stored) };
            } catch {
                return defaultSettings;
            }
        }
        return defaultSettings;
    });

    // Apply settings to document
    useEffect(() => {
        const root = document.documentElement;
        
        // Remove all font classes first
        Object.values(fontFamilyClasses).forEach(cls => root.classList.remove(cls));
        Object.values(fontSizeClasses).forEach(cls => root.classList.remove(cls));
        root.classList.remove('high-contrast', 'reduced-motion');
        
        // Apply current settings
        root.classList.add(fontFamilyClasses[settings.fontFamily]);
        root.classList.add(fontSizeClasses[settings.fontSize]);
        
        if (settings.highContrast) {
            root.classList.add('high-contrast');
        }
        
        if (settings.reducedMotion) {
            root.classList.add('reduced-motion');
        }
        
        // Save to localStorage
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    }, [settings]);

    const setFontFamily = (fontFamily: FontFamily) => {
        setSettings(prev => ({ ...prev, fontFamily }));
    };

    const setFontSize = (fontSize: FontSize) => {
        setSettings(prev => ({ ...prev, fontSize }));
    };

    const setHighContrast = (highContrast: boolean) => {
        setSettings(prev => ({ ...prev, highContrast }));
    };

    const setReducedMotion = (reducedMotion: boolean) => {
        setSettings(prev => ({ ...prev, reducedMotion }));
    };

    const resetSettings = () => {
        setSettings(defaultSettings);
    };

    return (
        <AccessibilityContext.Provider
            value={{
                ...settings,
                setFontFamily,
                setFontSize,
                setHighContrast,
                setReducedMotion,
                resetSettings,
            }}
        >
            {children}
        </AccessibilityContext.Provider>
    );
}

export function useAccessibility() {
    const context = useContext(AccessibilityContext);
    if (context === undefined) {
        throw new Error('useAccessibility must be used within an AccessibilityProvider');
    }
    return context;
}
