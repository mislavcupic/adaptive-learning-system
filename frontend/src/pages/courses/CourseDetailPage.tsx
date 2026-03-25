import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    ArrowLeft, BookOpen, Plus, Edit, Trash2, GripVertical, Code, Target
} from 'lucide-react';
import { Card, CardContent } from '../../components';
import { courseService } from '../../services';
import type { Course, LearningOutcome } from '../../types';

export const CourseDetailPage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { t } = useTranslation();
    const [course, setCourse] = useState<Course | null>(null);
    const [outcomes, setOutcomes] = useState<LearningOutcome[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showOutcomeModal, setShowOutcomeModal] = useState(false);
    const [editingOutcome, setEditingOutcome] = useState<LearningOutcome | null>(null);

    useEffect(() => {
        if (id) fetchCourseData();
    }, [id]);

    const fetchCourseData = async () => {
        try {
            setLoading(true);
            const [courseData, outcomesData] = await Promise.all([
                courseService.getById(id!),
                courseService.getOutcomes(id!)
            ]);
            setCourse(courseData);
            setOutcomes(outcomesData);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load course');
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteOutcome = async (outcomeId: string) => {
        if (!confirm('Jeste li sigurni da želite obrisati ovaj ishod učenja?')) return;
        try {
            await courseService.deleteOutcome(id!, outcomeId);
            setOutcomes(outcomes.filter(o => o.id !== outcomeId));
        } catch (err) {
            alert(err instanceof Error ? err.message : 'Failed to delete outcome');
        }
    };

    const handleOutcomeSaved = (outcome: LearningOutcome) => {
        if (editingOutcome) {
            setOutcomes(outcomes.map(o => o.id === outcome.id ? outcome : o));
        } else {
            setOutcomes([...outcomes, outcome]);
        }
        setShowOutcomeModal(false);
        setEditingOutcome(null);
    };

    if (loading) return <PageSkeleton />;

    if (error || !course) {
        return (
            <div className="p-6 text-center">
                <p className="text-red-500">{error || 'Course not found'}</p>
                <button 
                    onClick={() => navigate('/courses')}
                    className="mt-4 px-4 py-2 bg-violet-600 text-white rounded-lg"
                >
                    Natrag na kolegije
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/courses')}
                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                            {course.name}
                        </h1>
                        <span className={`text-sm px-2 py-0.5 rounded ${
                            course.languageType === 'C'
                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                                : 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300'
                        }`}>
                            {course.languageType === 'CSHARP' ? 'C#' : course.languageType}
                        </span>
                    </div>
                    {course.description && (
                        <p className="text-gray-500 dark:text-gray-400 mt-1">
                            {course.description}
                        </p>
                    )}
                </div>
                <button
                    onClick={() => navigate(`/courses/${id}/edit`)}
                    className="flex items-center gap-2 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
                >
                    <Edit className="w-4 h-4" />
                    Uredi
                </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card>
                    <CardContent className="flex items-center gap-4">
                        <div className="p-3 bg-violet-100 dark:bg-violet-900 rounded-lg">
                            <Target className="w-6 h-6 text-violet-600 dark:text-violet-400" />
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-gray-900 dark:text-white">
                                {outcomes.length}
                            </p>
                            <p className="text-sm text-gray-500">Ishoda učenja</p>
                        </div>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent className="flex items-center gap-4">
                        <div className="p-3 bg-blue-100 dark:bg-blue-900 rounded-lg">
                            <Code className="w-6 h-6 text-blue-600 dark:text-blue-400" />
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-gray-900 dark:text-white">
                                {outcomes.reduce((sum, o) => sum + o.taskCount, 0)}
                            </p>
                            <p className="text-sm text-gray-500">Ukupno zadataka</p>
                        </div>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent className="flex items-center gap-4">
                        <div className={`p-3 rounded-lg ${
                            course.isActive 
                                ? 'bg-green-100 dark:bg-green-900' 
                                : 'bg-gray-100 dark:bg-gray-700'
                        }`}>
                            <BookOpen className={`w-6 h-6 ${
                                course.isActive 
                                    ? 'text-green-600 dark:text-green-400' 
                                    : 'text-gray-600 dark:text-gray-400'
                            }`} />
                        </div>
                        <div>
                            <p className="text-lg font-semibold text-gray-900 dark:text-white">
                                {course.isActive ? 'Aktivan' : 'Neaktivan'}
                            </p>
                            <p className="text-sm text-gray-500">Status kolegija</p>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Learning Outcomes */}
            <div>
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                        Ishodi učenja
                    </h2>
                    <button
                        onClick={() => { setEditingOutcome(null); setShowOutcomeModal(true); }}
                        className="flex items-center gap-2 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                    >
                        <Plus className="w-4 h-4" />
                        Dodaj ishod
                    </button>
                </div>

                {outcomes.length > 0 ? (
                    <div className="space-y-3">
                        {outcomes.sort((a, b) => a.orderIndex - b.orderIndex).map((outcome) => (
                            <Card key={outcome.id} className="hover:border-violet-500 transition-colors">
                                <CardContent className="flex items-center gap-4">
                                    <div className="cursor-move text-gray-400 hover:text-gray-600">
                                        <GripVertical className="w-5 h-5" />
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2">
                                            <span className="text-sm font-medium text-violet-600 dark:text-violet-400">
                                                IU{outcome.orderIndex + 1}
                                            </span>
                                            <h3 className="font-medium text-gray-900 dark:text-white">
                                                {outcome.name}
                                            </h3>
                                        </div>
                                        {outcome.description && (
                                            <p className="text-sm text-gray-500 mt-1">
                                                {outcome.description}
                                            </p>
                                        )}
                                    </div>
                                    <div className="text-sm text-gray-500">
                                        {outcome.taskCount} zadataka
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <button
                                            onClick={() => navigate(`/tasks?outcomeId=${outcome.id}`)}
                                            className="px-3 py-1 text-sm text-violet-600 hover:bg-violet-50 dark:hover:bg-violet-900/20 rounded"
                                        >
                                            Zadaci
                                        </button>
                                        <button
                                            onClick={() => { setEditingOutcome(outcome); setShowOutcomeModal(true); }}
                                            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                                        >
                                            <Edit className="w-4 h-4 text-gray-500" />
                                        </button>
                                        <button
                                            onClick={() => handleDeleteOutcome(outcome.id)}
                                            className="p-2 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                                        >
                                            <Trash2 className="w-4 h-4 text-red-500" />
                                        </button>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-12 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg">
                        <Target className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                        <p className="text-gray-500 dark:text-gray-400">
                            Još nema ishoda učenja
                        </p>
                        <button
                            onClick={() => setShowOutcomeModal(true)}
                            className="mt-4 text-violet-600 hover:text-violet-700 font-medium"
                        >
                            + Dodaj prvi ishod
                        </button>
                    </div>
                )}
            </div>

            {/* Outcome Modal */}
            {showOutcomeModal && (
                <OutcomeModal
                    courseId={id!}
                    outcome={editingOutcome}
                    nextIndex={outcomes.length}
                    onClose={() => { setShowOutcomeModal(false); setEditingOutcome(null); }}
                    onSaved={handleOutcomeSaved}
                />
            )}
        </div>
    );
};

// Outcome Modal
interface OutcomeModalProps {
    courseId: string;
    outcome: LearningOutcome | null;
    nextIndex: number;
    onClose: () => void;
    onSaved: (outcome: LearningOutcome) => void;
}

const OutcomeModal = ({ courseId, outcome, nextIndex, onClose, onSaved }: OutcomeModalProps) => {
    const [name, setName] = useState(outcome?.name || '');
    const [description, setDescription] = useState(outcome?.description || '');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) {
            setError('Naziv je obavezan');
            return;
        }

        try {
            setLoading(true);
            let savedOutcome: LearningOutcome;
            if (outcome) {
                savedOutcome = await courseService.updateOutcome(courseId, outcome.id, { name, description });
            } else {
                savedOutcome = await courseService.createOutcome(courseId, { 
                    name, 
                    description, 
                    orderIndex: nextIndex 
                });
            }
            onSaved(savedOutcome);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to save outcome');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg w-full max-w-md p-6 m-4">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    {outcome ? 'Uredi ishod učenja' : 'Novi ishod učenja'}
                </h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {error && (
                        <div className="p-3 bg-red-100 text-red-700 rounded-lg text-sm">
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Naziv *
                        </label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            placeholder="npr. Rad s petljama"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Opis
                        </label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        />
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
                        >
                            Odustani
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50"
                        >
                            {loading ? 'Spremanje...' : 'Spremi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

const PageSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/3"></div>
        <div className="grid grid-cols-3 gap-4">
            {[...Array(3)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
        <div className="space-y-3">
            {[...Array(4)].map((_, i) => (
                <div key={i} className="h-20 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
    </div>
);

export default CourseDetailPage;
