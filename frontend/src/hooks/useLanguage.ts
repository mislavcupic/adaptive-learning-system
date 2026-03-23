// ============================================
// USE LANGUAGE HOOK - Promjena jezika
// ============================================

import { useTranslation } from 'react-i18next';
import { useCallback } from 'react';

type Language = 'hr' | 'en';

export const useLanguage = () => {
    const { i18n } = useTranslation();

    const currentLanguage = i18n.language as Language;

    const changeLanguage = useCallback(
        (lang: Language) => {
            i18n.changeLanguage(lang);
        },
        [i18n]
    );

    const toggleLanguage = useCallback(() => {
        const newLang = currentLanguage === 'hr' ? 'en' : 'hr';
        changeLanguage(newLang);
    }, [currentLanguage, changeLanguage]);

    return {
        currentLanguage,
        changeLanguage,
        toggleLanguage,
        isHr: currentLanguage === 'hr',
        isEn: currentLanguage === 'en',
    };
};