import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import hr from "./locales/hr.ts";
import en from "./locales/en.ts";

i18n
    .use(LanguageDetector) // Automatski detektira jezik browsera
    .use(initReactI18next)
    .init({
        resources: {
            hr: { translation: hr },
            en: { translation: en },
        },
        fallbackLng: 'hr', // Default jezik
        supportedLngs: ['hr', 'en'],

        interpolation: {
            escapeValue: false, // React već escapa
        },

        detection: {
            order: ['localStorage', 'navigator'],
            caches: ['localStorage'],
        },
    });