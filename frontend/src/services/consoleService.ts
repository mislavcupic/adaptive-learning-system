import { API_BASE_URL } from '../api/endpoints';

export type ConsoleExitReason = 'finished' | 'timeout' | 'stopped' | 'output_limit';

export type ConsoleServerMessage =
    | { type: 'status'; state: 'compiling' | 'running'; seconds?: number }
    | { type: 'output'; data: string }
    | { type: 'compiler_output'; output: string }
    | { type: 'compile_error'; output: string }
    | { type: 'exit'; reason: ConsoleExitReason; exitCode: number | null }
    | { type: 'error'; message: string };

export interface ConsoleHandle {
    sendLine: (line: string) => void;
    sendEof: () => void;
    stop: () => void;
    close: () => void;
}

interface OpenConsoleOptions {
    taskId: string;
    examId?: string;
    code: string;
    onMessage: (message: ConsoleServerMessage) => void;
    onClose: () => void;
}

const consoleUrl = (): string => {
    const url = new URL(API_BASE_URL, window.location.origin);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    url.pathname = `${url.pathname.replace(/\/$/, '')}/ws/run`;
    url.search = '';
    return url.toString();
};

export const consoleService = {
    open: ({ taskId, examId, code, onMessage, onClose }: OpenConsoleOptions): ConsoleHandle => {
        const socket = new WebSocket(consoleUrl());

        const send = (payload: Record<string, unknown>) => {
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify(payload));
            }
        };

        socket.onopen = () => {
            send({
                type: 'start',
                token: localStorage.getItem('accessToken'),
                taskId,
                examId,
                code,
            });
        };

        socket.onmessage = (event: MessageEvent<string>) => {
            let message: ConsoleServerMessage;
            try {
                message = JSON.parse(event.data);
            } catch {
                return;
            }
            onMessage(message);
        };

        socket.onclose = () => onClose();

        return {
            sendLine: (line: string) => send({ type: 'input', data: `${line}\n` }),
            sendEof: () => send({ type: 'eof' }),
            stop: () => send({ type: 'stop' }),
            close: () => socket.close(),
        };
    },
};
