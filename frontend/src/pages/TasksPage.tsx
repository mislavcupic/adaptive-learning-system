import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Search, Play, Clock, Award, Pencil } from 'lucide-react';
import { useAuth } from '../context';
import { useFetch } from '../hooks';
import { outcomeService, taskService } from '../services';
import {
    Card,
    CardContent,
    Button,
    Input,
    Badge,
    LoadingScreen,
    ErrorState,
    EmptyState
} from '../components/ui';
import { formatDate, formatLanguageType, truncate } from '../utils';
import { cn } from '../utils/cn';
import type { LearningOutcome, Task } from '../types';

export function TasksPage() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const [search, setSearch] = useState('');
    const [searchParams] = useSearchParams();
    const outcomeId = searchParams.get('outcomeId');

    const { data: tasks, loading, error, refetch } = useFetch<Task[]>(
        () => taskService.getAll(),
        []
    );

    // Redoslijed ishoda unutar kolegija; ako dohvat ne uspije, grupe se
    // slazu po nazivu ishoda.
    const { data: outcomes } = useFetch<LearningOutcome[]>(
        () => outcomeService.getAll().catch(() => []),
        []
    );

    const [dragId, setDragId] = useState<string | null>(null);
    const [orderOverride, setOrderOverride] = useState<string[] | null>(null);
    const [reorderError, setReorderError] = useState<string | null>(null);

    const outcomeTasks = sortByOrder(outcomeId
        ? tasks?.filter(task => task.outcomeId === outcomeId) ?? []
        : tasks ?? []);

    const orderedTasks = orderOverride
        ? [...outcomeTasks].sort((a, b) => orderOverride.indexOf(a.id) - orderOverride.indexOf(b.id))
        : outcomeTasks;

    const query = search.toLowerCase();
    const filteredTasks = orderedTasks.filter(task =>
        task.title.toLowerCase().includes(query) ||
        task.description?.toLowerCase().includes(query) ||
        task.outcomeName?.toLowerCase().includes(query)
    );

    const outcomeOrder = new Map((outcomes ?? []).map(o => [o.id, o.orderIndex ?? 0]));
    const taskGroups = groupByOutcome(filteredTasks, outcomeOrder);

    const isTeacherOrAdmin = user?.role === 'TEACHER' || user?.role === 'ADMIN';
    const canReorder = Boolean(outcomeId) && isTeacherOrAdmin && !search;

    const handleDrop = async (targetId: string) => {
        const draggedId = dragId;
        setDragId(null);
        if (!outcomeId || !draggedId || draggedId === targetId) return;

        const ids = orderedTasks.map(task => task.id);
        ids.splice(ids.indexOf(draggedId), 1);
        ids.splice(ids.indexOf(targetId), 0, draggedId);

        setOrderOverride(ids);
        setReorderError(null);
        try {
            await taskService.reorder(outcomeId, ids);
            await refetch();
        } catch {
            setReorderError(t('tasks.reorderFailed'));
        } finally {
            setOrderOverride(null);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('tasks.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {outcomeTasks.length} {t('exams.tasks')}
                    </p>
                    {outcomeId && (
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {t('tasks.filteredByOutcome', 'Prikazani su zadaci odabranog ishoda učenja.')}{' '}
                            <Link to="/tasks" className="underline">
                                {t('tasks.showAll', 'Prikaži sve')}
                            </Link>
                        </p>
                    )}
                    {canReorder && (
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {t('tasks.dragHint')}
                        </p>
                    )}
                    {reorderError && (
                        <p className="text-sm text-red-600 dark:text-red-400 mt-1">{reorderError}</p>
                    )}
                </div>
                {isTeacherOrAdmin && (
                    <Link to="/tasks/new">
                        <Button className="gap-2">
                            <Plus className="w-4 h-4" />
                            {t('tasks.createTask')}
                        </Button>
                    </Link>
                )}
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
                <Input
                    placeholder={t('common.search')}
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="pl-10"
                />
            </div>

            {/* Tasks Grid */}
            {filteredTasks.length === 0 ? (
                <EmptyState
                    title={t('tasks.noTasks')}
                    description={search ? t('common.noData') : t('tasks.noTasksDesc')}
                    action={
                        !search && isTeacherOrAdmin && (
                            <Link to="/tasks/new">
                                <Button>{t('tasks.createTask')}</Button>
                            </Link>
                        )
                    }
                />
            ) : (
                <div className="space-y-6">
                    {taskGroups.map((group) => (
                        <section
                            key={group.outcomeId}
                            className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 p-4 sm:p-5 space-y-4"
                        >
                            <div className="flex items-center gap-3 flex-wrap">
                                <span className="w-1 h-6 rounded-full bg-teal-600 dark:bg-teal-400" aria-hidden="true" />
                                <h2 className="text-lg font-semibold text-zinc-900 dark:text-white">
                                    {group.outcomeName}
                                </h2>
                                {group.courseName && (
                                    <Badge variant="info">{group.courseName}</Badge>
                                )}
                                <span className="ml-auto rounded-full bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 px-2.5 py-0.5 text-xs font-medium text-zinc-600 dark:text-zinc-300">
                                    {group.tasks.length} {t('exams.tasks')}
                                </span>
                            </div>
                            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                                {group.tasks.map((task) => (
                                    <div
                                        key={task.id}
                                        draggable={canReorder}
                                        onDragStart={(e) => {
                                            e.dataTransfer.setData('text/plain', task.id);
                                            setDragId(task.id);
                                        }}
                                        onDragOver={canReorder ? (e) => e.preventDefault() : undefined}
                                        onDrop={canReorder ? (e) => { e.preventDefault(); handleDrop(task.id); } : undefined}
                                        onDragEnd={() => setDragId(null)}
                                        className={cn(canReorder && 'cursor-move', dragId === task.id && 'opacity-50')}
                                    >
                                        <TaskCard task={task} isStudent={user?.role === 'STUDENT'} />
                                    </div>
                                ))}
                            </div>
                        </section>
                    ))}
                </div>
            )}
        </div>
    );
}

function sortByOrder(list: Task[]): Task[] {
    const groups = new Map<string, Task[]>();
    list.forEach(task => {
        const key = task.outcomeId ?? '';
        groups.set(key, [...(groups.get(key) ?? []), task]);
    });
    return [...groups.values()].flatMap(group =>
        [...group].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
    );
}

interface TaskGroup {
    outcomeId: string;
    outcomeName: string;
    courseName?: string;
    tasks: Task[];
}

// Grupe idu po kolegiju, pa po redoslijedu ishoda u kolegiju; zadaci u
// grupi zadrzavaju redoslijed iz sortByOrder.
function groupByOutcome(list: Task[], outcomeOrder: Map<string, number>): TaskGroup[] {
    const groups = new Map<string, TaskGroup>();
    list.forEach(task => {
        const key = task.outcomeId ?? '';
        const group = groups.get(key) ?? {
            outcomeId: key,
            outcomeName: task.outcomeName ?? '',
            courseName: task.courseName,
            tasks: []
        };
        group.tasks.push(task);
        groups.set(key, group);
    });

    const position = (group: TaskGroup) => outcomeOrder.get(group.outcomeId) ?? Number.MAX_SAFE_INTEGER;

    return [...groups.values()].sort((a, b) =>
        (a.courseName ?? '').localeCompare(b.courseName ?? '', 'hr')
        || position(a) - position(b)
        || a.outcomeName.localeCompare(b.outcomeName, 'hr', { numeric: true })
    );
}

function TaskCard({ task, isStudent }: { task: Task; isStudent: boolean }) {
    const { t } = useTranslation();

    return (
        <Card hoverable>
            <CardContent className="pt-4">
                <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                        <Link to={isStudent ? `/tasks/${task.id}/solve` : `/tasks/${task.id}/edit`}>
                            <h3 className="font-medium text-zinc-900 dark:text-white hover:underline">
                                {task.title}
                            </h3>
                        </Link>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {truncate(task.description, 100) || t('common.noData')}
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2 mt-4 flex-wrap">
                    {task.languageType && (
                        <Badge>{formatLanguageType(task.languageType)}</Badge>
                    )}
                    {task.courseName && (
                        <Badge variant="info">{task.courseName}</Badge>
                    )}
                </div>

                <div className="flex items-center gap-4 mt-4 text-sm text-zinc-500 dark:text-zinc-400">
                    <span className="flex items-center gap-1">
                        <Award className="w-4 h-4" />
                        {task.maxScore} {t('assessments.points')}
                    </span>
                    <span className="flex items-center gap-1">
                        <Clock className="w-4 h-4" />
                        {task.timeLimitSeconds}s
                    </span>
                </div>

                <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                    <span className="text-sm text-zinc-400">
                        {formatDate(task.createdAt)}
                    </span>
                    {isStudent ? (
                        <Link to={`/tasks/${task.id}/solve`}>
                            <Button size="sm" className="gap-2">
                                <Play className="w-3 h-3" />
                                {t('tasks.solveTask')}
                            </Button>
                        </Link>
                    ) : (
                        <Link to={`/tasks/${task.id}/edit`}>
                            <Button size="sm" variant="secondary" className="gap-2">
                                <Pencil className="w-3 h-3" />
                                {t('common.edit')}
                            </Button>
                        </Link>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}