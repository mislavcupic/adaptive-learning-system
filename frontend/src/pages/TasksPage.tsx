import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Search, Play, Clock, Award } from 'lucide-react';
import { useAuth } from '../context';
import { useFetch } from '../hooks';
import { taskService } from '../services';
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
import type { Task } from '../types';

export function TasksPage() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const [search, setSearch] = useState('');

    const { data: tasks, loading, error, refetch } = useFetch<Task[]>(
        () => taskService.getAll(),
        []
    );

    const filteredTasks = tasks?.filter(task =>
        task.title.toLowerCase().includes(search.toLowerCase()) ||
        task.description?.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    const isTeacherOrAdmin = user?.role === 'TEACHER' || user?.role === 'ADMIN';

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
                        {tasks?.length || 0} zadataka
                    </p>
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
                    description={search ? 'Nema rezultata za pretragu' : t('tasks.noTasksDesc')}
                    action={
                        !search && isTeacherOrAdmin && (
                            <Link to="/tasks/new">
                                <Button>{t('tasks.createTask')}</Button>
                            </Link>
                        )
                    }
                />
            ) : (
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredTasks.map((task) => (
                        <TaskCard key={task.id} task={task} isStudent={user?.role === 'STUDENT'} />
                    ))}
                </div>
            )}
        </div>
    );
}

function TaskCard({ task, isStudent }: { task: Task; isStudent: boolean }) {
    const { t } = useTranslation();

    return (
        <Card hoverable>
            <CardContent className="pt-4">
                <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                        <Link to={isStudent ? `/tasks/${task.id}/solve` : `/tasks/${task.id}`}>
                            <h3 className="font-medium text-zinc-900 dark:text-white hover:underline">
                                {task.title}
                            </h3>
                        </Link>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {truncate(task.description, 100) || 'Nema opisa'}
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
                        {task.maxScore} bodova
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
                        <Link to={`/tasks/${task.id}`}>
                            <Button size="sm" variant="secondary">
                                Pregledaj
                            </Button>
                        </Link>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
