import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    ClipboardList, Plus, Search, Filter, MoreVertical, Edit, Trash2, Eye, 
    Play, Clock, Code, CheckCircle
} from 'lucide-react';
import { Card, CardContent } from '../../components';
import { taskService, courseService } from '../../services';
import type { Task, Course, LanguageType } from '../../types';

export const TasksPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const outcomeIdFilter = searchParams.get('outcomeId');

    const [tasks, setTasks] = useState<Task[]>([]);
    const [courses, setCourses] = useState<Course[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [courseFilter, setCourseFilter] = useState('');
    const [languageFilter, setLanguageFilter] = useState<LanguageType | ''>('');

    useEffect(() => {
        fetchData();
    }, [outcomeIdFilter]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const [tasksData, coursesData] = await Promise.all([
                taskService.getAll(outcomeIdFilter || undefined),
                courseService.getAll()
            ]);
            setTasks(tasksData);
            setCourses(coursesData);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load tasks');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('Jeste li sigurni da želite obrisati ovaj zadatak?')) return;
        try {
            await taskService.delete(id);
            setTasks(tasks.filter(t => t.id !== id));
        } catch (err) {
            alert(err instanceof Error ? err.message : 'Failed to delete task');
        }
    };

    const filteredTasks = tasks.filter(task => {
        const matchesSearch = task.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
            task.description?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesCourse = !courseFilter || task.courseId === courseFilter;
        const matchesLanguage = !languageFilter || task.languageType === languageFilter;
        return matchesSearch && matchesCourse && matchesLanguage;
    });

    if (loading) return <PageSkeleton />;

    if (error) {
        return (
            <div className="p-6 text-center">
                <p className="text-red-500">{error}</p>
                <button onClick={fetchData} className="mt-4 px-4 py-2 bg-violet-600 text-white rounded-lg">
                    Pokušaj ponovno
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                        {t('tasks.title', 'Zadaci')}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        {outcomeIdFilter 
                            ? 'Zadaci za odabrani ishod učenja'
                            : 'Svi zadaci za programiranje'}
                    </p>
                </div>
                <button
                    onClick={() => navigate('/tasks/new')}
                    className="flex items-center gap-2 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                >
                    <Plus className="w-5 h-5" />
                    Novi zadatak
                </button>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Pretraži zadatke..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                    />
                </div>
                <select
                    value={courseFilter}
                    onChange={(e) => setCourseFilter(e.target.value)}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Svi kolegiji</option>
                    {courses.map(c => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                </select>
                <select
                    value={languageFilter}
                    onChange={(e) => setLanguageFilter(e.target.value as LanguageType | '')}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Svi jezici</option>
                    <option value="C">C</option>
                    <option value="CSHARP">C#</option>
                </select>
            </div>

            {/* Tasks List */}
            {filteredTasks.length > 0 ? (
                <div className="space-y-4">
                    {filteredTasks.map((task) => (
                        <TaskCard
                            key={task.id}
                            task={task}
                            onView={() => navigate(`/tasks/${task.id}`)}
                            onEdit={() => navigate(`/tasks/${task.id}/edit`)}
                            onDelete={() => handleDelete(task.id)}
                            onStart={() => navigate(`/tasks/${task.id}/solve`)}
                        />
                    ))}
                </div>
            ) : (
                <div className="text-center py-12">
                    <ClipboardList className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">
                        {searchTerm || courseFilter || languageFilter 
                            ? 'Nema rezultata za vašu pretragu'
                            : 'Još nema zadataka'}
                    </p>
                </div>
            )}
        </div>
    );
};

interface TaskCardProps {
    task: Task;
    onView: () => void;
    onEdit: () => void;
    onDelete: () => void;
    onStart: () => void;
}

const TaskCard = ({ task, onView, onEdit, onDelete, onStart }: TaskCardProps) => {
    const [showMenu, setShowMenu] = useState(false);

    return (
        <Card className="hover:border-violet-500 transition-colors">
            <CardContent className="flex items-center gap-4">
                <div className={`p-3 rounded-lg ${
                    task.languageType === 'C' 
                        ? 'bg-blue-100 dark:bg-blue-900' 
                        : 'bg-purple-100 dark:bg-purple-900'
                }`}>
                    <Code className={`w-6 h-6 ${
                        task.languageType === 'C' 
                            ? 'text-blue-600 dark:text-blue-400' 
                            : 'text-purple-600 dark:text-purple-400'
                    }`} />
                </div>

                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-gray-900 dark:text-white truncate">
                            {task.title}
                        </h3>
                        <span className={`text-xs px-2 py-0.5 rounded ${
                            task.languageType === 'C'
                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                                : 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300'
                        }`}>
                            {task.languageType === 'CSHARP' ? 'C#' : task.languageType}
                        </span>
                    </div>
                    <div className="flex items-center gap-4 mt-1 text-sm text-gray-500">
                        <span>{task.courseName}</span>
                        <span>•</span>
                        <span>{task.outcomeName}</span>
                    </div>
                </div>

                <div className="flex items-center gap-4 text-sm text-gray-500">
                    <div className="flex items-center gap-1">
                        <Clock className="w-4 h-4" />
                        {task.timeLimitSeconds}s
                    </div>
                    <div className="flex items-center gap-1">
                        <CheckCircle className="w-4 h-4" />
                        {task.submissionCount} predaja
                    </div>
                </div>

                <button
                    onClick={onStart}
                    className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                >
                    <Play className="w-4 h-4" />
                    Riješi
                </button>

                <div className="relative">
                    <button 
                        onClick={() => setShowMenu(!showMenu)}
                        className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                    >
                        <MoreVertical className="w-5 h-5 text-gray-500" />
                    </button>
                    {showMenu && (
                        <div className="absolute right-0 mt-1 w-40 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-10">
                            <button
                                onClick={() => { onView(); setShowMenu(false); }}
                                className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700"
                            >
                                <Eye className="w-4 h-4" /> Pregledaj
                            </button>
                            <button
                                onClick={() => { onEdit(); setShowMenu(false); }}
                                className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700"
                            >
                                <Edit className="w-4 h-4" /> Uredi
                            </button>
                            <button
                                onClick={() => { onDelete(); setShowMenu(false); }}
                                className="flex items-center gap-2 w-full px-4 py-2 text-left text-red-600 hover:bg-red-50"
                            >
                                <Trash2 className="w-4 h-4" /> Obriši
                            </button>
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
};

const PageSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
        <div className="flex gap-4">
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded flex-1"></div>
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-40"></div>
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-32"></div>
        </div>
        <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
    </div>
);

export default TasksPage;
