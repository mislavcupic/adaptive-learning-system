import { useEffect, useRef, useState, KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Terminal, Play, Square, Eraser, CornerDownLeft } from 'lucide-react';
import { consoleService } from '../../services/consoleService';
import type { ConsoleHandle, ConsoleServerMessage } from '../../services/consoleService';

type ConsoleState = 'idle' | 'compiling' | 'running' | 'finished' | 'error';

interface ConsolePanelProps {
    taskId: string;
    examId?: string;
    getCode: () => string;
    note: string;
}

export function ConsolePanel({ taskId, examId, getCode, note }: ConsolePanelProps) {
    const { t } = useTranslation();
    const [state, setState] = useState<ConsoleState>('idle');
    const [output, setOutput] = useState('');
    const [compilerText, setCompilerText] = useState('');
    const [message, setMessage] = useState('');
    const [input, setInput] = useState('');

    const handleRef = useRef<ConsoleHandle | null>(null);
    const stateRef = useRef<ConsoleState>('idle');
    const outputRef = useRef<HTMLPreElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const changeState = (next: ConsoleState) => {
        stateRef.current = next;
        setState(next);
    };

    useEffect(() => {
        return () => handleRef.current?.close();
    }, []);

    useEffect(() => {
        if (outputRef.current) {
            outputRef.current.scrollTop = outputRef.current.scrollHeight;
        }
    }, [output]);

    const exitMessage = (reason: string, exitCode: number | null) => {
        switch (reason) {
            case 'timeout':
                return t('console.exitTimeout');
            case 'stopped':
                return t('console.exitStopped');
            case 'output_limit':
                return t('console.exitOutputLimit');
            default:
                return t('console.exitFinished', { code: exitCode ?? '-' });
        }
    };

    const handleMessage = (msg: ConsoleServerMessage) => {
        switch (msg.type) {
            case 'status':
                changeState(msg.state);
                if (msg.state === 'running') {
                    inputRef.current?.focus();
                }
                break;
            case 'output':
                setOutput(prev => prev + msg.data);
                break;
            case 'compiler_output':
                setCompilerText(msg.output);
                break;
            case 'compile_error':
                setCompilerText(msg.output);
                setMessage(t('console.compileError'));
                changeState('error');
                break;
            case 'exit':
                setMessage(exitMessage(msg.reason, msg.exitCode));
                changeState('finished');
                break;
            case 'error':
                setMessage(msg.message);
                changeState('error');
                break;
        }
    };

    const handleClose = () => {
        handleRef.current = null;
        if (stateRef.current === 'compiling' || stateRef.current === 'running') {
            setMessage(t('console.disconnected'));
            changeState('error');
        }
    };

    const run = () => {
        if (stateRef.current === 'compiling' || stateRef.current === 'running') return;
        const code = getCode();
        if (!code.trim()) {
            setMessage(t('console.noCode'));
            changeState('error');
            return;
        }
        handleRef.current?.close();
        setOutput('');
        setCompilerText('');
        setMessage('');
        setInput('');
        changeState('compiling');
        handleRef.current = consoleService.open({
            taskId,
            examId,
            code,
            onMessage: handleMessage,
            onClose: handleClose,
        });
    };

    const stop = () => handleRef.current?.stop();

    const sendInput = () => {
        if (stateRef.current !== 'running') return;
        handleRef.current?.sendLine(input);
        setInput('');
    };

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendInput();
        } else if (e.key === 'd' && e.ctrlKey) {
            e.preventDefault();
            handleRef.current?.sendEof();
        }
    };

    const busy = state === 'compiling' || state === 'running';
    const statusText = state === 'compiling' ? t('console.compiling')
        : state === 'running' ? t('console.running')
            : message;

    return (
        <div className="overflow-hidden rounded-2xl bg-[#17201e] dark:bg-[#0a0f0e] border border-transparent dark:border-zinc-800">
            <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 border-b border-white/10">
                <span className="flex items-center gap-2 font-mono text-sm font-medium text-zinc-100">
                    <Terminal className="w-4 h-4" />
                    {t('console.title')}
                </span>
                <div className="flex items-center gap-2">
                    {busy ? (
                        <button
                            type="button"
                            onClick={stop}
                            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium bg-red-600 text-white hover:bg-red-700"
                        >
                            <Square className="w-3.5 h-3.5" />
                            {t('console.stop')}
                        </button>
                    ) : (
                        <button
                            type="button"
                            onClick={run}
                            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium bg-teal-300 text-teal-950 hover:bg-teal-200"
                        >
                            <Play className="w-3.5 h-3.5" />
                            {t('console.run')}
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={() => { setOutput(''); setCompilerText(''); if (!busy) setMessage(''); }}
                        className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-zinc-300 hover:bg-white/10"
                    >
                        <Eraser className="w-3.5 h-3.5" />
                        {t('console.clear')}
                    </button>
                </div>
            </div>

            {compilerText && (
                <pre className="mx-5 mt-4 p-3 rounded-lg bg-amber-500/10 text-amber-200 text-xs font-mono whitespace-pre-wrap overflow-x-auto">
                    {compilerText}
                </pre>
            )}

            <pre
                ref={outputRef}
                aria-live="polite"
                className="h-64 overflow-auto px-5 py-4 font-mono text-sm text-zinc-100 whitespace-pre-wrap break-words"
            >
                {output || (state === 'idle' ? <span className="text-zinc-500">{t('console.idle')}</span> : null)}
            </pre>

            <div className="flex items-center gap-2 px-5 py-3 border-t border-white/10">
                <span className="font-mono text-teal-300">&gt;</span>
                <input
                    ref={inputRef}
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={state !== 'running'}
                    placeholder={t('console.inputPlaceholder')}
                    aria-label={t('console.inputPlaceholder')}
                    className="flex-1 bg-transparent font-mono text-sm text-zinc-100 placeholder:text-zinc-500 outline-none disabled:opacity-50"
                />
                <button
                    type="button"
                    onClick={sendInput}
                    disabled={state !== 'running'}
                    className="flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs font-medium text-zinc-300 hover:bg-white/10 disabled:opacity-40"
                >
                    <CornerDownLeft className="w-3.5 h-3.5" />
                    Enter
                </button>
                <button
                    type="button"
                    onClick={() => handleRef.current?.sendEof()}
                    disabled={state !== 'running'}
                    className="rounded-lg px-2.5 py-1 text-xs font-medium text-zinc-300 hover:bg-white/10 disabled:opacity-40"
                >
                    {t('console.eof')}
                </button>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-2 px-5 py-2.5 border-t border-white/10 text-xs">
                <span className={state === 'error' ? 'text-red-300' : 'text-zinc-400'}>{statusText}</span>
                <span className="text-amber-200">{note}</span>
            </div>
        </div>
    );
}
