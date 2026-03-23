import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

type FontMode = 'default' | 'typewriter' | 'dyslexic' | 'lexend';

interface FontContextType {
    fontMode: FontMode;
    setFontMode: (mode: FontMode) => void;
}

const FontContext = createContext<FontContextType | undefined>(undefined);

const FONT_STORAGE_KEY = 'preferred-font-mode';

export const FontProvider = ({ children }: { children: ReactNode }) => {
    const [fontMode, setFontModeState] = useState<FontMode>(() => {
        const saved = localStorage.getItem(FONT_STORAGE_KEY);
        return (saved as FontMode) || 'default';
    });

    const setFontMode = (mode: FontMode) => {
        setFontModeState(mode);
        localStorage.setItem(FONT_STORAGE_KEY, mode);
    };

    useEffect(() => {
        // Apply font class to body
        document.body.classList.remove(
            'font-mode-default',
            'font-mode-typewriter',
            'font-mode-dyslexic',
            'font-mode-lexend'
        );
        document.body.classList.add(`font-mode-${fontMode}`);
    }, [fontMode]);

    return (
        <FontContext.Provider value={{ fontMode, setFontMode }}>
            {children}
        </FontContext.Provider>
    );
};

export const useFont = () => {
    const context = useContext(FontContext);
    if (!context) {
        throw new Error('useFont must be used within FontProvider');
    }
    return context;
};