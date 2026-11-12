import type { Monaco } from '@monaco-editor/react';

const tokenRules = [
    { token: 'comment', foreground: '8fa39c', fontStyle: 'italic' },
    { token: 'keyword', foreground: '6ee7c8' },
    { token: 'keyword.directive', foreground: 'f5c987' },
    { token: 'string', foreground: '9cc4ff' },
    { token: 'number', foreground: 'f5c987' },
    { token: 'type', foreground: '6ee7c8' },
    { token: 'delimiter', foreground: 'c5d2cd' },
];

const sharedColors = {
    'editor.foreground': '#e6edea',
    'editorLineNumber.foreground': '#7f918b',
    'editorLineNumber.activeForeground': '#e6edea',
    'editorCursor.foreground': '#6ee7c8',
    'editor.selectionBackground': '#2f4a44',
    'editor.inactiveSelectionBackground': '#263935',
    'editorIndentGuide.background1': '#26312e',
    'editorIndentGuide.activeBackground1': '#3a4744',
    'editorWidget.background': '#1a2220',
    'editorWidget.border': '#26312e',
    'scrollbarSlider.background': '#3a474480',
    'scrollbarSlider.hoverBackground': '#4c5652a0',
    'scrollbarSlider.activeBackground': '#66716ca0',
};

export function defineEditorThemes(monaco: Monaco) {
    monaco.editor.defineTheme('adaptive-light', {
        base: 'vs-dark',
        inherit: true,
        rules: tokenRules,
        colors: {
            ...sharedColors,
            'editor.background': '#17201e',
            'editor.lineHighlightBackground': '#1f2a27',
            'editorGutter.background': '#17201e',
        },
    });

    monaco.editor.defineTheme('adaptive-dark', {
        base: 'vs-dark',
        inherit: true,
        rules: tokenRules,
        colors: {
            ...sharedColors,
            'editor.background': '#0a0f0e',
            'editor.lineHighlightBackground': '#141b19',
            'editorGutter.background': '#0a0f0e',
        },
    });
}

export function editorThemeName(theme: 'light' | 'dark') {
    return theme === 'dark' ? 'adaptive-dark' : 'adaptive-light';
}

export const editorBaseOptions = {
    minimap: { enabled: false },
    fontSize: 14,
    lineHeight: 24,
    fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
    fontLigatures: true,
    lineNumbers: 'on' as const,
    scrollBeyondLastLine: false,
    automaticLayout: true,
    tabSize: 4,
    padding: { top: 16, bottom: 16 },
    renderLineHighlight: 'line' as const,
    cursorBlinking: 'smooth' as const,
    smoothScrolling: true,
    overviewRulerBorder: false,
    scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },
};
