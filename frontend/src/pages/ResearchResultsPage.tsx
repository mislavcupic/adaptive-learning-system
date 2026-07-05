import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, Users, TrendingUp } from 'lucide-react';
import { researchService } from '../services';
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
import type { ResearchResult } from '../types';

export function ResearchResultsPage() {
    const { t } = useTranslation();

    const [results, setResults] = useState<ResearchResult[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadResults();
    }, []);

    const loadResults = async () => {
        try {
            setLoading(true);
            const data = await researchService.getResults();
            setResults(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    // Deskriptivni sažetak po grupi (broj + prosjek pretesta)
    const summaryFor = (group: 'CONTROL' | 'EXPERIMENTAL') => {
        const inGroup = results.filter(r => r.researchGroup === group);
        const withPretest = inGroup.filter(r => r.pretestPercentage != null);
        const avgPretest = withPretest.length > 0
            ? withPretest.reduce((sum, r) => sum + (r.pretestPercentage ?? 0), 0) / withPretest.length
            : null;
        const withPosttest = inGroup.filter(r => r.posttestPercentage != null);
        const avgPosttest = withPosttest.length > 0
            ? withPosttest.reduce((sum, r) => sum + (r.posttestPercentage ?? 0), 0) / withPosttest.length
            : null;
        return { count: inGroup.length, avgPretest, avgPosttest };
    };

    const control = summaryFor('CONTROL');
    const experimental = summaryFor('EXPERIMENTAL');

    // CSV export u "tidy" formatu — jedan red po studentu, spremno za ANCOVA-u
    const exportCsv = () => {
        const headers = [
            'student_id',
            'first_name',
            'last_name',
            'email',
            'research_group',
            'pretest_score',
            'pretest_max_score',
            'pretest_percentage',
            'posttest_score',
            'posttest_max_score',
            'posttest_percentage',
        ];

        const escape = (val: unknown): string => {
            if (val == null) return '';
            const s = String(val);
            // Ako sadrži zarez, navodnike ili novi red — omotaj u navodnike
            if (/[",\n]/.test(s)) {
                return `"${s.replace(/"/g, '""')}"`;
            }
            return s;
        };

        const rows = results.map(r => [
            r.studentId,
            r.firstName,
            r.lastName,
            r.email,
            r.researchGroup,
            r.pretestScore,
            r.pretestMaxScore,
            r.pretestPercentage != null ? r.pretestPercentage.toFixed(2) : '',
            r.posttestScore,
            r.posttestMaxScore,
            r.posttestPercentage != null ? r.posttestPercentage.toFixed(2) : '',
        ].map(escape).join(','));

        const csv = [headers.join(','), ...rows].join('\n');

        // BOM za ispravan prikaz dijakritike u Excelu
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const date = new Date().toISOString().slice(0, 10);
        link.download = `research-results-${date}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={loadResults} />;

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        Rezultati istraživanja
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        Usporedba kontrolne i eksperimentalne skupine
                    </p>
                </div>
                <Button
                    onClick={exportCsv}
                    disabled={results.length === 0}
                    className="gap-2"
                >
                    <Download className="w-4 h-4" />
                    Izvezi za analizu (CSV)
                </Button>
            </div>

            {/* Sažetak po grupama */}
            <div className="grid gap-4 sm:grid-cols-2">
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            <Users className="w-4 h-4 text-amber-500" />
                            Kontrolna skupina
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="flex items-center gap-6">
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {control.count}
                                </p>
                                <p className="text-sm text-zinc-500">ispitanika</p>
                            </div>
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {control.avgPretest != null ? `${control.avgPretest.toFixed(1)}%` : '—'}
                                </p>
                                <p className="text-sm text-zinc-500">prosjek pretesta</p>
                            </div>
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {control.avgPosttest != null ? `${control.avgPosttest.toFixed(1)}%` : '—'}
                                </p>
                                <p className="text-sm text-zinc-500">prosjek posttesta</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            <TrendingUp className="w-4 h-4 text-emerald-500" />
                            Eksperimentalna skupina
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="flex items-center gap-6">
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {experimental.count}
                                </p>
                                <p className="text-sm text-zinc-500">ispitanika</p>
                            </div>
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {experimental.avgPretest != null ? `${experimental.avgPretest.toFixed(1)}%` : '—'}
                                </p>
                                <p className="text-sm text-zinc-500">prosjek pretesta</p>
                            </div>
                            <div>
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {experimental.avgPosttest != null ? `${experimental.avgPosttest.toFixed(1)}%` : '—'}
                                </p>
                                <p className="text-sm text-zinc-500">prosjek posttesta</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Tablica po studentu */}
            <Card>
                <CardHeader>
                    <CardTitle>Rezultati po ispitaniku</CardTitle>
                </CardHeader>
                <CardContent>
                    {results.length === 0 ? (
                        <p className="text-center py-8 text-zinc-500 dark:text-zinc-400">
                            Nema još rezultata. Rezultati se pojavljuju kad ispitanici riješe pretest ili posttest.
                        </p>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                <tr className="border-b border-zinc-200 dark:border-zinc-700 text-left text-zinc-500">
                                    <th className="py-3 pr-4 font-medium">Ispitanik</th>
                                    <th className="py-3 pr-4 font-medium">Skupina</th>
                                    <th className="py-3 pr-4 font-medium text-right">Pretest</th>
                                    <th className="py-3 pr-4 font-medium text-right">Posttest</th>
                                </tr>
                                </thead>
                                <tbody>
                                {results.map(r => (
                                    <tr
                                        key={r.studentId}
                                        className="border-b border-zinc-100 dark:border-zinc-800 last:border-0"
                                    >
                                        <td className="py-3 pr-4">
                                            <p className="font-medium text-zinc-900 dark:text-white">
                                                {r.firstName} {r.lastName}
                                            </p>
                                            <p className="text-xs text-zinc-500">{r.email}</p>
                                        </td>
                                        <td className="py-3 pr-4">
                                            <Badge variant={r.researchGroup === 'EXPERIMENTAL' ? 'success' : 'warning'}>
                                                {r.researchGroup === 'EXPERIMENTAL' ? 'Eksperimentalna' : 'Kontrolna'}
                                            </Badge>
                                        </td>
                                        <td className="py-3 pr-4 text-right">
                                            {r.pretestPercentage != null ? (
                                                <span className="text-zinc-900 dark:text-white">
                                                        {r.pretestScore}/{r.pretestMaxScore}
                                                    <span className="text-zinc-400 ml-1">
                                                            ({r.pretestPercentage.toFixed(0)}%)
                                                        </span>
                                                    </span>
                                            ) : (
                                                <span className="text-zinc-400">—</span>
                                            )}
                                        </td>
                                        <td className="py-3 pr-4 text-right">
                                            {r.posttestPercentage != null ? (
                                                <span className="text-zinc-900 dark:text-white">
                                                        {r.posttestScore}/{r.posttestMaxScore}
                                                    <span className="text-zinc-400 ml-1">
                                                            ({r.posttestPercentage.toFixed(0)}%)
                                                        </span>
                                                    </span>
                                            ) : (
                                                <span className="text-zinc-400">—</span>
                                            )}
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