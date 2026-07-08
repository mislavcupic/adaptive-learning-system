import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, ShieldCheck, CheckCircle2, XCircle } from 'lucide-react';
import { auditService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    Badge,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import type { AuditLog } from '../types';

export function AuditLogsPage() {
    const { t } = useTranslation();

    const [logs, setLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadLogs();
    }, []);

    const loadLogs = async () => {
        try {
            setLoading(true);
            const data = await auditService.getLogs();
            setLogs(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (iso: string): string => {
        try {
            return new Date(iso).toLocaleString('hr-HR');
        } catch {
            return iso;
        }
    };

    const exportCsv = () => {
        const headers = [
            'created_at',
            'user_email',
            'user_role',
            'action',
            'http_method',
            'endpoint',
            'success',
            'error_message',
            'duration_ms',
            'ip_address',
        ];

        const escape = (val: unknown): string => {
            if (val == null) return '';
            const s = String(val);
            if (/[",\n]/.test(s)) {
                return `"${s.replace(/"/g, '""')}"`;
            }
            return s;
        };

        const rows = logs.map(l => [
            l.createdAt,
            l.userEmail,
            l.userRole,
            l.action,
            l.httpMethod,
            l.endpoint,
            l.success,
            l.errorMessage,
            l.durationMs,
            l.ipAddress,
        ].map(escape).join(','));

        const csv = [headers.join(','), ...rows].join('\n');

        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const date = new Date().toISOString().slice(0, 10);
        link.download = `audit-logs-${date}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={loadLogs} />;

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white flex items-center gap-2">
                        <ShieldCheck className="w-6 h-6 text-blue-500" />
                        {t('audit.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {t('audit.subtitle')}
                    </p>
                </div>
                <Button
                    onClick={exportCsv}
                    disabled={logs.length === 0}
                    className="gap-2"
                >
                    <Download className="w-4 h-4" />
                    {t('audit.exportCsv')}
                </Button>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>{t('audit.recordsTitle')}</CardTitle>
                </CardHeader>
                <CardContent>
                    {logs.length === 0 ? (
                        <p className="text-center py-8 text-zinc-500 dark:text-zinc-400">
                            {t('audit.noRecords')}
                        </p>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                <tr className="border-b border-zinc-200 dark:border-zinc-700 text-left text-zinc-500">
                                    <th className="py-3 pr-4 font-medium">{t('audit.time')}</th>
                                    <th className="py-3 pr-4 font-medium">{t('audit.user')}</th>
                                    <th className="py-3 pr-4 font-medium">{t('audit.action')}</th>
                                    <th className="py-3 pr-4 font-medium">{t('audit.endpoint')}</th>
                                    <th className="py-3 pr-4 font-medium text-center">{t('audit.status')}</th>
                                    <th className="py-3 pr-4 font-medium text-right">{t('audit.duration')}</th>
                                    <th className="py-3 pr-4 font-medium">{t('audit.ip')}</th>
                                </tr>
                                </thead>
                                <tbody>
                                {logs.map(l => (
                                    <tr
                                        key={l.id}
                                        className="border-b border-zinc-100 dark:border-zinc-800 last:border-0"
                                    >
                                        <td className="py-3 pr-4 whitespace-nowrap text-zinc-600 dark:text-zinc-400">
                                            {formatDate(l.createdAt)}
                                        </td>
                                        <td className="py-3 pr-4">
                                            {l.userEmail ? (
                                                <div>
                                                    <p className="text-zinc-900 dark:text-white">{l.userEmail}</p>
                                                    {l.userRole && (
                                                        <Badge variant="default">{l.userRole}</Badge>
                                                    )}
                                                </div>
                                            ) : (
                                                <span className="text-zinc-400">—</span>
                                            )}
                                        </td>
                                        <td className="py-3 pr-4">
                                            <span className="font-medium text-zinc-900 dark:text-white">
                                                {l.action}
                                            </span>
                                            {l.httpMethod && (
                                                <span className="text-xs text-zinc-400 ml-2">
                                                    {l.httpMethod}
                                                </span>
                                            )}
                                        </td>
                                        <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400 max-w-xs truncate">
                                            {l.endpoint || '—'}
                                        </td>
                                        <td className="py-3 pr-4 text-center">
                                            {l.success ? (
                                                <CheckCircle2 className="w-4 h-4 text-emerald-500 inline" />
                                            ) : (
                                                <span title={l.errorMessage || ''}>
                                                    <XCircle className="w-4 h-4 text-red-500 inline" />
                                                </span>
                                            )}
                                        </td>
                                        <td className="py-3 pr-4 text-right text-zinc-500 dark:text-zinc-400">
                                            {l.durationMs != null ? `${l.durationMs} ms` : '—'}
                                        </td>
                                        <td className="py-3 pr-4 text-zinc-500 dark:text-zinc-400">
                                            {l.ipAddress || '—'}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}