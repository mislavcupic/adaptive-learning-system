import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Save } from 'lucide-react';
import { courseService } from '../services';
import { Card, CardHeader, CardTitle, CardContent, Button, Input } from '../components/ui';

export function CoursesFormPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        name: '',
        description: '',
        languageType: 'C' as 'C' | 'CSHARP' | 'PYTHON',
    });
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const set = (field: string, value: any) =>
        setForm(f => ({ ...f, [field]: value }));

    const handleSubmit = async () => {
        if (!form.name.trim()) { setError(t('validation.required')); return; }
        setSubmitting(true);
        setError(null);
        try {
            const course = await courseService.create(form);
            navigate(`/courses/${course.id}`);
        } catch {
            setError(t('errors.generic'));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate(-1)} className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800">
                        <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {t('courses.createCourse')}
                        </h1>
                    </div>
                </div>
                <div className="flex gap-3">
                    <Button variant="ghost" onClick={() => navigate(-1)}>{t('common.cancel')}</Button>
                    <Button onClick={handleSubmit} isLoading={submitting} className="gap-2">
                        <Save className="w-4 h-4" />
                        {t('common.save')}
                    </Button>
                </div>
            </div>

            {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-400 text-sm">
                    {error}
                </div>
            )}

            <Card>
                <CardHeader><CardTitle>{t('courses.createCourse')}</CardTitle></CardHeader>
                <CardContent className="space-y-4">
                    <Input
                        label={`${t('courses.courseName')} *`}
                        value={form.name}
                        onChange={e => set('name', e.target.value)}
                        placeholder="npr. Programiranje u C-u"
                    />
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                            {t('common.description')} ({t('common.optional')})
                        </label>
                        <textarea
                            value={form.description}
                            onChange={e => set('description', e.target.value)}
                            rows={3}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                            {t('courses.language')} *
                        </label>
                        <select
                            value={form.languageType}
                            onChange={e => set('languageType', e.target.value)}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                        >
                            <option value="C">C</option>
                            <option value="CSHARP">C#</option>
                            <option value="PYTHON">Python</option>
                        </select>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

export default CoursesFormPage