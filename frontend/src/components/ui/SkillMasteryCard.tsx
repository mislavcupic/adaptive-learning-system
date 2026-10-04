import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Brain, TrendingUp, Target } from 'lucide-react';
import { masteryService } from '../../services';
import { Card, CardHeader, CardTitle, CardContent } from './Card';
import type { SkillMastery } from '../../types';

interface Props {
    /** Ako je zadan, dohvaća se profil tog studenta (profesorski pogled). */
    studentId?: string;
    /** Prikaz bez okvira kartice, za ugradnju u postojeću karticu. */
    bare?: boolean;
}

/**
 * Prikaz BKT procjene znanja po vještinama.
 *
 * mastery_level je vjerojatnost da student zna koncept, pa se prikazuje
 * kao postotak. Vrijednosti dolaze iz BKT modela koji se ažurira nakon
 * svake predaje.
 */
export function SkillMasteryCard({ studentId, bare = false }: Props) {
    const { t } = useTranslation();

    const [skills, setSkills] = useState<SkillMastery[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        load();
    }, [studentId]);

    const load = async () => {
        try {
            setLoading(true);
            const data = studentId
                ? await masteryService.getByStudent(studentId)
                : await masteryService.getMy();
            setSkills(data);
        } catch {
            setSkills([]);
        } finally {
            setLoading(false);
        }
    };

    const content = (
        <>
            {loading ? (
                <p className="text-sm text-zinc-400 py-4 text-center">
                    {t('common.loading')}
                </p>
            ) : skills.length === 0 ? (
                <p className="text-sm text-zinc-400 py-4 text-center">
                    {t('mastery.noData')}
                </p>
            ) : (
                <div className="space-y-4">
                    {skills.map(skill => (
                        <SkillBar key={skill.id} skill={skill} />
                    ))}

                    <div className="pt-3 border-t border-zinc-100 dark:border-zinc-800">
                        <p className="text-xs text-zinc-400">
                            {t('mastery.explanation')}
                        </p>
                    </div>
                </div>
            )}
        </>
    );

    if (bare) return content;

    return (
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <Brain className="w-4 h-4 text-zinc-400" />
                    {t('mastery.title')}
                </CardTitle>
            </CardHeader>
            <CardContent>{content}</CardContent>
        </Card>
    );
}

// ======================================================================

function SkillBar({ skill }: { skill: SkillMastery }) {
    const { t } = useTranslation();

    const percent = Math.round(skill.masteryLevel * 100);

    // Pragovi prate interpretaciju iz BKT servisa
    const level =
        percent >= 95 ? 'mastered'
            : percent >= 80 ? 'strong'
                : percent >= 60 ? 'developing'
                    : percent >= 40 ? 'weak'
                        : 'beginning';

    const barColor = {
        mastered: 'bg-emerald-500',
        strong: 'bg-emerald-400',
        developing: 'bg-blue-400',
        weak: 'bg-amber-400',
        beginning: 'bg-red-400',
    }[level];

    const textColor = {
        mastered: 'text-emerald-700 dark:text-emerald-400',
        strong: 'text-emerald-700 dark:text-emerald-400',
        developing: 'text-blue-700 dark:text-blue-400',
        weak: 'text-amber-700 dark:text-amber-400',
        beginning: 'text-red-700 dark:text-red-400',
    }[level];

    return (
        <div>
            <div className="flex items-center justify-between mb-1.5 gap-3">
                <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                    {skill.skillName}
                </p>
                <span className={`text-sm font-semibold shrink-0 ${textColor}`}>
                    {percent}%
                </span>
            </div>

            <div className="h-2 rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
                <div
                    className={`h-full rounded-full transition-all duration-500 ${barColor}`}
                    style={{ width: `${percent}%` }}
                />
            </div>

            <div className="flex items-center justify-between mt-1.5">
                <span className={`text-xs ${textColor}`}>
                    {t(`mastery.level.${level}`)}
                </span>
                <span className="text-xs text-zinc-400 flex items-center gap-1">
                    <Target className="w-3 h-3" />
                    {skill.correctCount}/{skill.attemptsCount} {t('mastery.correct')}
                </span>
            </div>
        </div>
    );
}

// ======================================================================

/**
 * Kompaktni prikaz promjene nakon jedne predaje.
 *
 * Koristi se odmah ispod rezultata, da student vidi kako je zadatak
 * utjecao na procjenu znanja.
 */
export function MasteryDelta({ skillName, before, after }: {
    skillName: string;
    before: number;
    after: number;
}) {
    const { t } = useTranslation();

    const beforePct = Math.round(before * 100);
    const afterPct = Math.round(after * 100);
    const rose = afterPct >= beforePct;

    return (
        <div className="p-4 rounded-lg bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700">
            <p className="text-xs font-semibold text-zinc-500 uppercase tracking-wide mb-2 flex items-center gap-1.5">
                <TrendingUp className="w-3.5 h-3.5" />
                {t('mastery.afterSubmission')}
            </p>

            <p className="text-sm text-zinc-700 dark:text-zinc-300">
                {skillName}
            </p>

            <div className="flex items-center gap-2 mt-1.5">
                <span className="text-lg text-zinc-400">{beforePct}%</span>
                <span className="text-zinc-300">→</span>
                <span className={`text-lg font-semibold ${
                    rose ? 'text-emerald-600' : 'text-amber-600'
                }`}>
                    {afterPct}%
                </span>
            </div>
        </div>
    );
}