import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, Users, TrendingUp, BarChart3, CheckCircle2, AlertCircle } from 'lucide-react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
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
import type { ResearchResult, AncovaResponse } from '../types';

export function ResearchResultsPage() {
    const { t } = useTranslation();

    const [results, setResults] = useState<ResearchResult[]>([]);
    const [ancova, setAncova] = useState<AncovaResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [ancovaLoading, setAncovaLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadResults();
        loadAncova();
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

    const loadAncova = async () => {
        try {
            setAncovaLoading(true);
            const data = await researchService.getAncova();
            setAncova(data);
        } catch {
            // ANCOVA nije kritična za prikaz ostatka stranice
            setAncova(null);
        } finally {
            setAncovaLoading(false);
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

    // Podaci za graf — prosjeci pretest/posttest po grupi
    const chartData = ancova?.descriptives?.map(d => ({
        group: d.group === 'EXPERIMENTAL' ? 'Eksperimentalna' : 'Kontrolna',
        Pretest: d.pretest_mean,
        Posttest: d.posttest_mean,
    })) ?? [];

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

    const groupEffect = ancova?.ancova?.group_effect ?? null;

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

            {/* ANCOVA statistička analiza */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <BarChart3 className="w-5 h-5 text-blue-500" />
                        Statistička analiza (ANCOVA)
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    {ancovaLoading ? (
                        <p className="text-center py-8 text-zinc-500">Računanje…</p>
                    ) : ancova?.ancova && groupEffect ? (
                        <div className="space-y-6">
                            {/* Zaključak */}
                            <div className={`flex items-start gap-3 p-4 rounded-lg border ${
                                ancova.ancova.significant
                                    ? 'border-emerald-200 bg-emerald-50 dark:border-emerald-800 dark:bg-emerald-900/20'
                                    : 'border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20'
                            }`}>
                                {ancova.ancova.significant ? (
                                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                                ) : (
                                    <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                                )}
                                <div className="text-sm">
                                    {ancova.ancova.significant ? (
                                        <p className="text-zinc-700 dark:text-zinc-300">
                                            Razlika između skupina je <strong>statistički značajna</strong> (p {groupEffect.p != null && groupEffect.p < 0.001 ? '< 0.001' : `= ${groupEffect.p?.toFixed(3)}`}),
                                            uz kontrolu početne razine znanja (pretest kao kovarijat).
                                        </p>
                                    ) : (
                                        <p className="text-zinc-700 dark:text-zinc-300">
                                            Razlika između skupina <strong>nije statistički značajna</strong> (p = {groupEffect.p?.toFixed(3)}),
                                            uz kontrolu početne razine znanja.
                                        </p>
                                    )}
                                    <p className="text-xs text-zinc-500 mt-1">
                                        n = {ancova.n_total} ispitanika. Ovo je automatski izračun; za rad provjerite i pretpostavke ANCOVA.
                                    </p>
                                </div>
                            </div>

                            {/* Ključne vrijednosti */}
                            <div className="grid grid-cols-3 gap-4">
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {groupEffect.f != null ? groupEffect.f.toFixed(2) : '—'}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">F (grupa)</p>
                                </div>
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {groupEffect.p != null
                                            ? (groupEffect.p < 0.001 ? '< 0.001' : groupEffect.p.toFixed(3))
                                            : '—'}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">p-vrijednost</p>
                                </div>
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {groupEffect.partial_eta_sq != null ? groupEffect.partial_eta_sq.toFixed(3) : '—'}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">parcijalni η²</p>
                                </div>
                            </div>

                            {/* Graf prosjeka */}
                            {chartData.length > 0 && (
                                <div className="h-72 w-full">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#e4e4e7" />
                                            <XAxis dataKey="group" tick={{ fontSize: 12 }} />
                                            <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} unit="%" />
                                            <Tooltip formatter={(v) => `${Number(v).toFixed(1)}%`} />                                            <Legend />
                                            <Bar dataKey="Pretest" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="Posttest" fill="#10b981" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            )}
                        </div>
                    ) : (
                        <p className="text-center py-8 text-zinc-500 dark:text-zinc-400">
                            {ancova?.warning ??
                                'Nema dovoljno podataka za ANCOVA analizu. Potrebni su ispitanici u obje skupine koji su riješili i pretest i posttest.'}
                        </p>
                    )}
                </CardContent>
            </Card>

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