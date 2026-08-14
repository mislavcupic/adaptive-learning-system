import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
    Download, Users, TrendingUp, BarChart3, CheckCircle2, AlertCircle,
    Ruler, Scale, ShieldCheck, XCircle
} from 'lucide-react';
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
import type { ResearchResult, AncovaResponse, AssumptionTest } from '../types';

const MAGNITUDE_LABEL: Record<string, string> = {
    negligible: 'zanemariv',
    small: 'mali',
    medium: 'srednji',
    large: 'velik',
};

const GROUP_LABEL = (g: string) =>
    g === 'EXPERIMENTAL' ? 'Eksperimentalna' : g === 'CONTROL' ? 'Kontrolna' : g;

const fmtP = (p: number | null | undefined) => {
    if (p == null) return '—';
    return p < 0.001 ? '< 0,001' : p.toFixed(3).replace('.', ',');
};

const fmtNum = (v: number | null | undefined, digits = 2) => {
    if (v == null) return '—';
    return v.toFixed(digits).replace('.', ',');
};

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
            setAncova(null);
        } finally {
            setAncovaLoading(false);
        }
    };

    // Sazetak po skupini.
    // Ako postoje ANCOVA deskriptivi, koristi njih - oni opisuju TOCNO onaj
    // uzorak nad kojim je analiza provedena (samo ispitanici s oba testa).
    // Inace racunaj iz sirovih rezultata, uz preskakanje degeneriranih zapisa
    // gdje je max_score bio 0.
    const summaryFor = (group: 'CONTROL' | 'EXPERIMENTAL') => {
        const desc = ancova?.descriptives?.find(d => d.group === group);
        if (desc) {
            return {
                count: desc.n,
                avgPretest: desc.pretest_mean,
                avgPosttest: desc.posttest_mean,
                fromAnalysis: true,
            };
        }

        const inGroup = results.filter(r => r.researchGroup === group);
        const valid = inGroup.filter(
            r => (r.pretestMaxScore ?? 0) > 0 && (r.posttestMaxScore ?? 0) > 0
                && r.pretestPercentage != null && r.posttestPercentage != null
        );
        const avg = (pick: (r: ResearchResult) => number | null) =>
            valid.length > 0
                ? valid.reduce((s, r) => s + (pick(r) ?? 0), 0) / valid.length
                : null;

        return {
            count: valid.length,
            avgPretest: avg(r => r.pretestPercentage),
            avgPosttest: avg(r => r.posttestPercentage),
            fromAnalysis: false,
        };
    };

    const control = summaryFor('CONTROL');
    const experimental = summaryFor('EXPERIMENTAL');

    // Broj ispitanika koji su prijavljeni, ali nisu usli u analizu
    const excludedCount = results.filter(
        r => (r.researchGroup === 'CONTROL' || r.researchGroup === 'EXPERIMENTAL')
            && !((r.pretestMaxScore ?? 0) > 0 && (r.posttestMaxScore ?? 0) > 0
                && r.pretestPercentage != null && r.posttestPercentage != null)
    ).length;

    const chartData = ancova?.descriptives?.map(d => ({
        group: GROUP_LABEL(d.group),
        Pretest: d.pretest_mean,
        Posttest: d.posttest_mean,
    })) ?? [];

    const exportCsv = () => {
        const headers = [
            'student_id', 'first_name', 'last_name', 'email', 'research_group',
            'pretest_score', 'pretest_max_score', 'pretest_percentage',
            'posttest_score', 'posttest_max_score', 'posttest_percentage',
        ];

        const escape = (val: unknown): string => {
            if (val == null) return '';
            const s = String(val);
            if (/[",\n]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
            return s;
        };

        const rows = results.map(r => [
            r.studentId, r.firstName, r.lastName, r.email, r.researchGroup,
            r.pretestScore, r.pretestMaxScore,
            r.pretestPercentage != null ? r.pretestPercentage.toFixed(2) : '',
            r.posttestScore, r.posttestMaxScore,
            r.posttestPercentage != null ? r.posttestPercentage.toFixed(2) : '',
        ].map(escape).join(','));

        const csv = [headers.join(','), ...rows].join('\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `research-results-${new Date().toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={loadResults} />;

    const groupEffect = ancova?.ancova?.group_effect ?? null;
    const effect = ancova?.effect_size ?? null;
    const adjusted = ancova?.adjusted_means ?? null;
    const assumptions = ancova?.assumptions ?? null;

    const assumptionRows: Array<{ key: string; label: string; test: AssumptionTest | null; hint: string }> =
        assumptions
            ? [
                {
                    key: 'slopes',
                    label: 'Homogenost regresijskih nagiba',
                    test: assumptions.homogeneity_of_slopes,
                    hint: 'Interakcija skupina × pretest ne smije biti značajna.',
                },
                {
                    key: 'levene',
                    label: 'Homogenost varijanci (Levene)',
                    test: assumptions.levene,
                    hint: 'Raspršenost posttesta treba biti slična u obje skupine.',
                },
                {
                    key: 'shapiro',
                    label: 'Normalnost reziduala (Shapiro-Wilk)',
                    test: assumptions.shapiro,
                    hint: 'Odstupanja od modela trebaju biti približno normalno raspoređena.',
                },
            ]
            : [];

    const allAssumptionsOk = assumptionRows.length > 0
        && assumptionRows.every(a => a.test?.satisfied !== false);

    const renderSummaryCard = (
        title: string,
        icon: React.ReactNode,
        s: ReturnType<typeof summaryFor>
    ) => (
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                    {icon}
                    {title}
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className="flex items-center gap-6">
                    <div>
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {s.count}
                        </p>
                        <p className="text-sm text-zinc-500">ispitanika</p>
                    </div>
                    <div>
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {s.avgPretest != null ? `${fmtNum(s.avgPretest, 1)}%` : '—'}
                        </p>
                        <p className="text-sm text-zinc-500">prosjek pretesta</p>
                    </div>
                    <div>
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {s.avgPosttest != null ? `${fmtNum(s.avgPosttest, 1)}%` : '—'}
                        </p>
                        <p className="text-sm text-zinc-500">prosjek posttesta</p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );

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
                <Button onClick={exportCsv} disabled={results.length === 0} className="gap-2">
                    <Download className="w-4 h-4" />
                    Izvezi za analizu (CSV)
                </Button>
            </div>

            {/* Sazetak po skupinama */}
            <div className="grid gap-4 sm:grid-cols-2">
                {renderSummaryCard(
                    'Kontrolna skupina',
                    <Users className="w-4 h-4 text-amber-500" />,
                    control
                )}
                {renderSummaryCard(
                    'Eksperimentalna skupina',
                    <TrendingUp className="w-4 h-4 text-emerald-500" />,
                    experimental
                )}
            </div>

            {excludedCount > 0 && (
                <p className="text-xs text-zinc-500 dark:text-zinc-400 -mt-2">
                    Prikazani su ispitanici uključeni u statističku analizu.
                    {' '}{excludedCount} {excludedCount === 1 ? 'ispitanik nije riješio' : 'ispitanika nije riješilo'} oba
                    testa pa {excludedCount === 1 ? 'nije uključen' : 'nisu uključeni'} u izračun.
                </p>
            )}

            {/* ANCOVA */}
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
                                    <p className="text-zinc-700 dark:text-zinc-300">
                                        Razlika između skupina{' '}
                                        <strong>
                                            {ancova.ancova.significant
                                                ? 'je statistički značajna'
                                                : 'nije statistički značajna'}
                                        </strong>{' '}
                                        (p {fmtP(groupEffect.p)}), uz kontrolu početne razine znanja
                                        (pretest kao kovarijata).
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">
                                        n = {ancova.n_total} ispitanika.
                                    </p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {fmtNum(groupEffect.f)}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">
                                        F({groupEffect.df ?? '—'},{' '}
                                        {ancova.ancova.table?.find(r => r.source === 'Residual')?.df ?? '—'})
                                    </p>
                                </div>
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {fmtP(groupEffect.p)}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">p-vrijednost</p>
                                </div>
                                <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                    <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                        {fmtNum(groupEffect.partial_eta_sq, 3)}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-1">parcijalni η²</p>
                                </div>
                            </div>

                            {chartData.length > 0 && (
                                <div className="h-72 w-full">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#e4e4e7" />
                                            <XAxis dataKey="group" tick={{ fontSize: 12 }} />
                                            <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} unit="%" />
                                            <Tooltip formatter={(v) => `${Number(v).toFixed(1)}%`} />
                                            <Legend />
                                            <Bar dataKey="Pretest" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="Posttest" fill="#10b981" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            )}

                            {/* ANCOVA tablica */}
                            {ancova.ancova.table && ancova.ancova.table.length > 0 && (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                        <tr className="border-b border-zinc-200 dark:border-zinc-700 text-left text-zinc-500">
                                            <th className="py-2 pr-4 font-medium">Izvor</th>
                                            <th className="py-2 pr-4 font-medium text-right">SS</th>
                                            <th className="py-2 pr-4 font-medium text-right">df</th>
                                            <th className="py-2 pr-4 font-medium text-right">MS</th>
                                            <th className="py-2 pr-4 font-medium text-right">F</th>
                                            <th className="py-2 pr-4 font-medium text-right">p</th>
                                            <th className="py-2 font-medium text-right">η²ₚ</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {ancova.ancova.table.map(row => (
                                            <tr key={row.source} className="border-b border-zinc-100 dark:border-zinc-800 last:border-0">
                                                <td className="py-2 pr-4 text-zinc-900 dark:text-white">
                                                    {row.source === 'group' ? 'Skupina'
                                                        : row.source === 'pretest' ? 'Pretest (kovarijata)'
                                                            : 'Rezidual'}
                                                </td>
                                                <td className="py-2 pr-4 text-right">{fmtNum(row.ss)}</td>
                                                <td className="py-2 pr-4 text-right">{row.df ?? '—'}</td>
                                                <td className="py-2 pr-4 text-right">
                                                    {row.ss != null && row.df ? fmtNum(row.ss / row.df) : '—'}
                                                </td>
                                                <td className="py-2 pr-4 text-right">{fmtNum(row.f)}</td>
                                                <td className="py-2 pr-4 text-right">{fmtP(row.p)}</td>
                                                <td className="py-2 text-right">{fmtNum(row.partial_eta_sq, 3)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
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

            {/* Velicina ucinka */}
            {effect && (
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Ruler className="w-5 h-5 text-violet-500" />
                            Veličina učinka
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid gap-4 sm:grid-cols-4">
                            <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                    {fmtNum(effect.hedges_g, 3)}
                                </p>
                                <p className="text-xs text-zinc-500 mt-1">Hedgesov g</p>
                            </div>
                            <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                <p className="text-xl font-semibold text-zinc-900 dark:text-white">
                                    {fmtNum(effect.cohens_d, 3)}
                                </p>
                                <p className="text-xs text-zinc-500 mt-1">Cohenov d</p>
                            </div>
                            <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                <p className="text-sm font-semibold text-zinc-900 dark:text-white pt-1">
                                    [{fmtNum(effect.ci_lower)}; {fmtNum(effect.ci_upper)}]
                                </p>
                                <p className="text-xs text-zinc-500 mt-1.5">95% interval pouzdanosti</p>
                            </div>
                            <div className="text-center p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50">
                                <div className="pt-0.5">
                                    <Badge variant={
                                        effect.magnitude === 'large' ? 'success'
                                            : effect.magnitude === 'medium' ? 'info'
                                                : 'warning'
                                    }>
                                        {MAGNITUDE_LABEL[effect.magnitude] ?? effect.magnitude}
                                    </Badge>
                                </div>
                                <p className="text-xs text-zinc-500 mt-2">veličina učinka</p>
                            </div>
                        </div>

                        <p className="text-sm text-zinc-600 dark:text-zinc-400">
                            Razlika prilagođenih sredina iznosi {fmtNum(effect.mean_difference)} postotnih bodova
                            u korist {effect.favors === 'EXPERIMENTAL' ? 'eksperimentalne' : 'kontrolne'} skupine,
                            uz objedinjenu standardnu devijaciju od {fmtNum(effect.pooled_sd)}.
                            {effect.ci_lower > 0 || effect.ci_upper < 0
                                ? ' Interval pouzdanosti ne obuhvaća nulu, što potvrđuje značajnost učinka.'
                                : ' Interval pouzdanosti obuhvaća nulu, pa učinak treba tumačiti s oprezom.'}
                        </p>
                    </CardContent>
                </Card>
            )}

            {/* Prilagodjene sredine */}
            {adjusted && adjusted.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Scale className="w-5 h-5 text-sky-500" />
                            Prilagođene aritmetičke sredine
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-4">
                            Vrijednosti koje bi skupine postigle na posttestu da su krenule s jednakim početnim znanjem.
                        </p>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                <tr className="border-b border-zinc-200 dark:border-zinc-700 text-left text-zinc-500">
                                    <th className="py-2 pr-4 font-medium">Skupina</th>
                                    <th className="py-2 pr-4 font-medium text-right">n</th>
                                    <th className="py-2 pr-4 font-medium text-right">Opažena sredina</th>
                                    <th className="py-2 pr-4 font-medium text-right">Prilagođena sredina</th>
                                    <th className="py-2 font-medium text-right">SE</th>
                                </tr>
                                </thead>
                                <tbody>
                                {adjusted.map(a => (
                                    <tr key={a.group} className="border-b border-zinc-100 dark:border-zinc-800 last:border-0">
                                        <td className="py-2 pr-4">
                                            <Badge variant={a.group === 'EXPERIMENTAL' ? 'success' : 'warning'}>
                                                {GROUP_LABEL(a.group)}
                                            </Badge>
                                        </td>
                                        <td className="py-2 pr-4 text-right">{a.n}</td>
                                        <td className="py-2 pr-4 text-right">{fmtNum(a.observed_mean)}</td>
                                        <td className="py-2 pr-4 text-right font-medium text-zinc-900 dark:text-white">
                                            {fmtNum(a.adjusted_mean)}
                                        </td>
                                        <td className="py-2 text-right">{fmtNum(a.se, 3)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Pretpostavke */}
            {assumptionRows.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <ShieldCheck className={`w-5 h-5 ${allAssumptionsOk ? 'text-emerald-500' : 'text-amber-500'}`} />
                            Provjera pretpostavki
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        {!allAssumptionsOk && (
                            <div className="flex items-start gap-3 p-3 rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20">
                                <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                                <p className="text-sm text-zinc-700 dark:text-zinc-300">
                                    Barem jedna pretpostavka nije zadovoljena. Rezultat ANCOVE i dalje je izračunat,
                                    ali ga u radu treba tumačiti s oprezom i navesti ovo ograničenje.
                                </p>
                            </div>
                        )}

                        {assumptionRows.map(({ key, label, test, hint }) => (
                            <div
                                key={key}
                                className="flex items-start justify-between gap-4 p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50"
                            >
                                <div className="flex items-start gap-3 min-w-0">
                                    {test == null ? (
                                        <AlertCircle className="w-5 h-5 text-zinc-400 shrink-0 mt-0.5" />
                                    ) : test.satisfied ? (
                                        <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                                    ) : (
                                        <XCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
                                    )}
                                    <div className="min-w-0">
                                        <p className="font-medium text-zinc-900 dark:text-white">{label}</p>
                                        <p className="text-xs text-zinc-500 mt-0.5">
                                            {test?.note ?? hint}
                                        </p>
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-sm font-medium text-zinc-900 dark:text-white">
                                        p = {fmtP(test?.p)}
                                    </p>
                                    {test?.f != null && (
                                        <p className="text-xs text-zinc-500">F = {fmtNum(test.f)}</p>
                                    )}
                                    {test?.statistic != null && (
                                        <p className="text-xs text-zinc-500">W = {fmtNum(test.statistic, 3)}</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}

            {/* Tablica po ispitaniku */}
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
                                    <th className="py-3 font-medium text-center">U analizi</th>
                                </tr>
                                </thead>
                                <tbody>
                                {results.map(r => {
                                    const inAnalysis =
                                        (r.researchGroup === 'CONTROL' || r.researchGroup === 'EXPERIMENTAL')
                                        && (r.pretestMaxScore ?? 0) > 0 && (r.posttestMaxScore ?? 0) > 0
                                        && r.pretestPercentage != null && r.posttestPercentage != null;

                                    return (
                                        <tr
                                            key={r.studentId}
                                            className={`border-b border-zinc-100 dark:border-zinc-800 last:border-0 ${
                                                inAnalysis ? '' : 'opacity-60'
                                            }`}
                                        >
                                            <td className="py-3 pr-4">
                                                <p className="font-medium text-zinc-900 dark:text-white">
                                                    {r.firstName} {r.lastName}
                                                </p>
                                                <p className="text-xs text-zinc-500">{r.email}</p>
                                            </td>
                                            <td className="py-3 pr-4">
                                                <Badge variant={r.researchGroup === 'EXPERIMENTAL' ? 'success' : 'warning'}>
                                                    {GROUP_LABEL(r.researchGroup)}
                                                </Badge>
                                            </td>
                                            <td className="py-3 pr-4 text-right">
                                                {r.pretestPercentage != null && (r.pretestMaxScore ?? 0) > 0 ? (
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
                                                {r.posttestPercentage != null && (r.posttestMaxScore ?? 0) > 0 ? (
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
                                            <td className="py-3 text-center">
                                                {inAnalysis ? (
                                                    <CheckCircle2 className="w-4 h-4 text-emerald-600 inline" />
                                                ) : (
                                                    <span className="text-zinc-400 text-xs">—</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}