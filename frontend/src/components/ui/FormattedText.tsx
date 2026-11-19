import { cn } from '../../utils/cn';

interface FormattedTextProps {
    text?: string | null;
    className?: string;
}

interface Segment {
    code: boolean;
    text: string;
}

const TYPE_START = /^(int|float|double|char|void|long|short|unsigned|signed|const|struct|bool)\s+\**\s*[A-Za-z_]\w*\s*[([=;,]/;
const STATEMENT_START = /^(if|for|while|switch|return|printf|scanf|puts|getchar|putchar)\s*[(;]/;
const INLINE_CODE_START = /[?.:!]\s+(?=(#\s*include\b|(int|float|double|char|void|long|short|unsigned|struct)\s+\**\s*[A-Za-z_]\w*\s*[([=;,]|(printf|scanf|if|for|while)\s*\())/g;

function isCodeLine(line: string): boolean {
    const t = line.trim();
    if (!t) return false;
    if (/^#\s*(include|define)\b/.test(t)) return true;
    if (/[{}]/.test(t)) return true;
    if (/;\s*$/.test(t)) return true;
    if (/^else\b/.test(t)) return true;
    return TYPE_START.test(t) || STATEMENT_START.test(t);
}

function splitMixedLine(line: string): string[] {
    INLINE_CODE_START.lastIndex = 0;
    let match: RegExpExecArray | null;
    while ((match = INLINE_CODE_START.exec(line)) !== null) {
        const cut = match.index + match[0].length;
        const prose = line.slice(0, match.index + 1);
        const code = line.slice(cut);
        if (/[{;]/.test(code) && !isCodeLine(prose)) {
            return [prose, code];
        }
    }
    return [line];
}

function formatC(src: string): string {
    const out: string[] = [];
    let line = '';
    let depth = 0;
    let paren = 0;
    let quote: string | null = null;

    const push = () => {
        const trimmed = line.trim();
        if (trimmed) out.push('    '.repeat(Math.max(depth, 0)) + trimmed);
        line = '';
    };

    for (let i = 0; i < src.length; i++) {
        const ch = src[i];

        if (quote) {
            line += ch;
            if (ch === '\\' && i + 1 < src.length) {
                line += src[++i];
            } else if (ch === quote) {
                quote = null;
            }
            continue;
        }

        if (!line.trim() && /\s/.test(ch)) continue;

        if (ch === '"' || ch === "'") {
            quote = ch;
            line += ch;
            continue;
        }

        if (ch === '(') paren++;
        if (ch === ')') paren = Math.max(paren - 1, 0);

        if (ch === '{') {
            line += '{';
            push();
            depth++;
            continue;
        }

        if (ch === '}') {
            push();
            depth--;
            line = '}';
            let j = i + 1;
            while (j < src.length && /\s/.test(src[j])) j++;
            const rest = src.slice(j);
            if (!rest.startsWith(';') && !/^(else|while)\b/.test(rest)) push();
            continue;
        }

        if (ch === ';' && paren === 0) {
            line += ';';
            push();
            continue;
        }

        line += ch;

        if (ch === '>' && /^#\s*include\s*</.test(line.trim())) push();
    }

    push();
    return out.join('\n');
}

function isCrammed(line: string): boolean {
    const t = line.trim();
    const brace = t.indexOf('{');
    if (brace >= 0 && t.slice(brace + 1).trim()) return true;
    const statements = (t.replace(/\(([^()]*)\)/g, '()').match(/;/g) ?? []).length;
    return statements > 1;
}

function shouldReformat(code: string): boolean {
    return code.split('\n').some(isCrammed);
}

function trimBlankLines(text: string): string {
    return text.replace(/^\s*\n/, '').replace(/\n\s*$/, '');
}

function toSegments(text: string): Segment[] {
    const lines = text.replace(/\r\n/g, '\n').split('\n').flatMap(splitMixedLine);
    const segments: Segment[] = [];

    for (const line of lines) {
        const blank = !line.trim();
        const code = blank ? segments[segments.length - 1]?.code ?? false : isCodeLine(line);
        const last = segments[segments.length - 1];
        if (last && last.code === code) {
            last.text += '\n' + line;
        } else {
            segments.push({ code, text: line });
        }
    }

    return segments
        .map(segment => {
            const cleaned = trimBlankLines(segment.text);
            return {
                code: segment.code,
                text: segment.code && shouldReformat(cleaned.trim()) ? formatC(cleaned.trim()) : cleaned,
            };
        })
        .filter(segment => segment.text.trim());
}

export function FormattedText({ text, className }: FormattedTextProps) {
    if (!text) return null;

    return (
        <div className={cn('space-y-2', className)}>
            {toSegments(text).map((segment, index) =>
                segment.code ? (
                    <pre
                        key={index}
                        className="overflow-x-auto rounded-xl bg-[#17201e] px-4 py-3 font-mono text-sm font-normal leading-relaxed text-zinc-100 whitespace-pre"
                    >
                        <code>{segment.text}</code>
                    </pre>
                ) : (
                    <p key={index} className="whitespace-pre-wrap">
                        {segment.text}
                    </p>
                )
            )}
        </div>
    );
}
