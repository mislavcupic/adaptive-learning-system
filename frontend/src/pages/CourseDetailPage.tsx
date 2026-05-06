import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Plus, Trash2, ChevronRight } from 'lucide-react';
import { useFetch } from '../hooks';
import { courseService, outcomeService } from '../services';
import {
    Card, CardHeader, CardTitle, CardContent,
    Button, Input, LoadingScreen, ErrorState, Badge, Modal, ModalFooter
} from '../components/ui';
import type { Course, LearningOutcome } from '../types';

export function CourseDetailPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [outcomeModal, setOutcomeModal] = useState(false);
    const [outcomeForm, setOutcomeForm] = useState({ name: '', description: '', orderIndex: 0 });
    const [savingOutcome, setSavingOutcome] = useState(false);
    const [deleteOutcomeModal, setDeleteOutcomeModal] = useState<LearningOutcome | null>(null);
    const [deletingOutcome, setDeletingOutcome] = useState(false);

    const { data: course, loading, error, refetch } = useFetch<Course>(
        () => courseService.getById(id!),
        [id]
    );

    const { data: outcomes, refetch: refetchOutcomes } = useFetch<LearningOutcome[]>(
        () => courseService.getOutcomes(id!),
        [id]
    );

    const handleCreateOutcome = async () => {
        if (!outcomeForm.name.trim() || !id) return;
        setSavingOutcome(true);
        try {
            await outcomeService.create({
                name: outcomeForm.name,
                description: outcomeForm.description || undefined,
                courseId: id as any,
                orderIndex: outcomeForm.orderIndex,
            });
            setOutcomeModal(false);
            setOutcomeForm({ name: '', description: '', orderIndex: 0 });
            refetchOutcomes();
        } catch {
            // handle error
        } finally {
            setSavingOutcome(false);
        }
    };

    const handleDeleteOutcome = async () => {
        if (!deleteOutcomeModal) return;
        setDeletingOutcome(true);
        try {
            await outcomeService.delete(deleteOutcomeModal.id);
            setDeleteOutcomeModal(null);
            refetchOutcomes();
        } finally {
            setDeletingOutcome(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error || !course) return <ErrorState description={error || ''} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate('/courses')} className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800">
                        <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {course.name}
                        </h1>
                        <div className="flex items-center gap-2 mt-1">
                            <Badge>{course.languageType === 'CSHARP' ? 'C#' : course.languageType}</Badge>
                            <Badge variant={course.isActive ? 'success' : 'default'}>
                                {course.isActive ? t('common.active') : t('common.inactive')}
                            </Badge>
                        </div>
                    </div>
                </div>
            </div>

            {/* Course info */}
            {course.description && (
                <Card>
                    <CardContent className="pt-4">
                        <p className="text-zinc-600 dark:text-zinc-400 text-sm">{course.description}</p>
                    </CardContent>
                </Card>
            )}

            {/* Learning Outcomes */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <CardTitle>{t('courses.outcomes')}</CardTitle>
                        <Button size="sm" className="gap-2" onClick={() => setOutcomeModal(true)}>
                            <Plus className="w-4 h-4" />
                            {t('common.add')} {t('courses.outcomes').toLowerCase()}
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    {!outcomes || outcomes.length === 0 ? (
                        <div className="text-center py-8 text-zinc-500 dark:text-zinc-400 text-sm">
                            Još nema ishoda učenja. Dodajte prvi ishod.
                        </div>
                    ) : (
                        <div className="space-y-2">
                            {outcomes.sort((a, b) => a.orderIndex - b.orderIndex).map((outcome, idx) => (
                                <div
                                    key={outcome.id}
                                    className="flex items-center justify-between p-3 rounded-lg border border-zinc-200 dark:border-zinc-700 hover:border-zinc-300 dark:hover:border-zinc-600 transition-colors"
                                >
                                    <div className="flex items-center gap-3 flex-1 min-w-0">
                                        <span className="text-xs font-mono text-zinc-400 w-6">{idx + 1}.</span>
                                        <div className="min-w-0">
                                            <p className="font-medium text-zinc-900 dark:text-white text-sm truncate">
                                                {outcome.name}
                                            </p>
                                            {outcome.description && (
                                                <p className="text-xs text-zinc-500 dark:text-zinc-400 truncate">
                                                    {outcome.description}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 flex-shrink-0">
                                        <Link to={`/tasks?outcomeId=${outcome.id}`}>
                                            <Button variant="ghost" size="sm" className="gap-1 text-xs">
                                                Zadaci
                                                <ChevronRight className="w-3 h-3" />
                                            </Button>
                                        </Link>
                                        <button
                                            onClick={() => setDeleteOutcomeModal(outcome)}
                                            className="p-1.5 rounded hover:bg-red-50 dark:hover:bg-red-900/20 text-zinc-400 hover:text-red-600 transition-colors"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Add Outcome Modal */}
            <Modal
                isOpen={outcomeModal}
                onClose={() => { setOutcomeModal(false); setOutcomeForm({ name: '', description: '', orderIndex: 0 }); }}
                title="Novi ishod učenja"
            >
                <div className="space-y-4">
                    <Input
                        label="Naziv ishoda *"
                        value={outcomeForm.name}
                        onChange={e => setOutcomeForm(f => ({ ...f, name: e.target.value }))}
                        placeholder="npr. Razumijevanje petlji"
                    />
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                            {t('common.description')} ({t('common.optional')})
                        </label>
                        <textarea
                            value={outcomeForm.description}
                            onChange={e => setOutcomeForm(f => ({ ...f, description: e.target.value }))}
                            rows={3}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                            Redosljed
                        </label>
                        <input
                            type="number"
                            value={outcomeForm.orderIndex}
                            onChange={e => setOutcomeForm(f => ({ ...f, orderIndex: Number(e.target.value) }))}
                            min={0}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                        />
                    </div>
                </div>
                <ModalFooter>
                    <Button variant="ghost" onClick={() => setOutcomeModal(false)}>{t('common.cancel')}</Button>
                    <Button onClick={handleCreateOutcome} isLoading={savingOutcome}>{t('common.create')}</Button>
                </ModalFooter>
            </Modal>

            {/* Delete Outcome Modal */}
            <Modal
                isOpen={!!deleteOutcomeModal}
                onClose={() => setDeleteOutcomeModal(null)}
                title={t('common.confirm')}
            >
                <p className="text-zinc-600 dark:text-zinc-400">
                    Ishod "{deleteOutcomeModal?.name}" će biti trajno obrisan zajedno sa svim zadacima.
                </p>
                <ModalFooter>
                    <Button variant="ghost" onClick={() => setDeleteOutcomeModal(null)}>{t('common.cancel')}</Button>
                    <Button variant="danger" onClick={handleDeleteOutcome} isLoading={deletingOutcome}>{t('common.delete')}</Button>
                </ModalFooter>
            </Modal>
        </div>
    );
}