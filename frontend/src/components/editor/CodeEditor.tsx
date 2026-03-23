import { useRef } from 'react';
import Editor, {type OnMount } from '@monaco-editor/react';
import type { editor } from 'monaco-editor';

interface CodeEditorProps {
    value: string;
    onChange: (value: string) => void;
    language?: 'c' | 'csharp';
    readOnly?: boolean;
    height?: string;
    theme?: 'vs-dark' | 'light';
}

export const CodeEditor = ({
                               value,
                               onChange,
                               language = 'c',
                               readOnly = false,
                               height = '400px',
                               theme = 'vs-dark',
                           }: CodeEditorProps) => {
    const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);

    const handleEditorMount: OnMount = (editor) => {
        editorRef.current = editor;
        editor.focus();
    };

    const handleChange = (newValue: string | undefined) => {
        onChange(newValue || '');
    };

    const monacoLanguage = language === 'csharp' ? 'csharp' : 'c';

    return (
        <div className="border border-gray-700 rounded-lg overflow-hidden">
            <Editor
                height={height}
                language={monacoLanguage}
                value={value}
                theme={theme}
                onChange={handleChange}
                onMount={handleEditorMount}
                options={{
                    readOnly,
                    minimap: { enabled: false },
                    fontSize: 14,
                    tabSize: 4,
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    lineNumbers: 'on',
                    wordWrap: 'on',
                    folding: true,
                    renderWhitespace: 'selection',
                    bracketPairColorization: { enabled: true },
                }}
            />
        </div>
    );
};