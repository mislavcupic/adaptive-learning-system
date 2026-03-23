// TASK SERVICE - Zadaci, ispiti, test caseovi


import { api, ENDPOINTS } from '../api';
import type { Task, Exam, TestCase, GradingCriteria, TaskType } from '../types';

export const taskService = {


    /**
     * Dohvati sve zadatke
     */
    getAll: async (params?: {
        outcomeId?: string;
        taskType?: TaskType;
        difficulty?: number;
    }): Promise<Task[]> => {
        const queryParams = new URLSearchParams();
        if (params?.outcomeId) queryParams.append('outcomeId', params.outcomeId);
        if (params?.taskType) queryParams.append('taskType', params.taskType);
        if (params?.difficulty) queryParams.append('difficulty', params.difficulty.toString());

        const endpoint = queryParams.toString()
            ? `${ENDPOINTS.TASKS.BASE}?${queryParams}`
            : ENDPOINTS.TASKS.BASE;

        const response = await api.get<Task[]>(endpoint);
        return response.data;
    },

    /**
     * Dohvati zadatak po ID-u
     */
    getById: async (id: string): Promise<Task> => {
        const response = await api.get<Task>(ENDPOINTS.TASKS.BY_ID(id));
        return response.data;
    },

    /**
     * Dohvati zadatke za određeni ishod
     */
    getByOutcome: async (outcomeId: string): Promise<Task[]> => {
        const response = await api.get<Task[]>(ENDPOINTS.TASKS.BY_OUTCOME(outcomeId));
        return response.data;
    },

    /**
     * Kreiraj novi zadatak
     */
    create: async (data: {
        outcomeId: string;
        title: string;
        description: string;
        taskType: TaskType;
        difficulty: 1 | 2 | 3 | 4 | 5;
        starterCode?: string;
        expectedSolution?: string;
        testCases: Omit<TestCase, 'id'>[];
        gradingCriteria: Omit<GradingCriteria, 'id' | 'taskId'>[];
        timeLimitMinutes?: number;
        availableFrom?: string;
        availableTo?: string;
    }): Promise<Task> => {
        const response = await api.post<Task>(ENDPOINTS.TASKS.BASE, data);
        return response.data;
    },

    /**
     * Ažuriraj zadatak
     */
    update: async (id: string, data: Partial<Task>): Promise<Task> => {
        const response = await api.put<Task>(ENDPOINTS.TASKS.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši zadatak
     */
    delete: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.TASKS.BY_ID(id));
    },

    /**
     * AI generiranje zadatka na temelju ishoda i težine
     */
    generateTask: async (data: {
        outcomeId: string;
        difficulty: 1 | 2 | 3 | 4 | 5;
        taskType: TaskType;
        additionalInstructions?: string;
    }): Promise<Task> => {
        const response = await api.post<Task>(ENDPOINTS.TASKS.GENERATE, data);
        return response.data;
    },



    /**
     * Dohvati sve ispite
     */
    getAllExams: async (courseId?: string): Promise<Exam[]> => {
        const endpoint = courseId
            ? ENDPOINTS.EXAMS.BY_COURSE(courseId)
            : ENDPOINTS.EXAMS.BASE;

        const response = await api.get<Exam[]>(endpoint);
        return response.data;
    },

    /**
     * Dohvati ispit po ID-u
     */
    getExamById: async (id: string): Promise<Exam> => {
        const response = await api.get<Exam>(ENDPOINTS.EXAMS.BY_ID(id));
        return response.data;
    },

    /**
     * Kreiraj novi ispit
     */
    createExam: async (data: {
        courseId: string;
        title: string;
        description: string;
        taskIds: string[];
        durationMinutes: number;
        availableFrom: string;
        availableTo: string;
        classIds: string[];
    }): Promise<Exam> => {
        const response = await api.post<Exam>(ENDPOINTS.EXAMS.BASE, data);
        return response.data;
    },

    /**
     * Ažuriraj ispit
     */
    updateExam: async (id: string, data: Partial<Exam>): Promise<Exam> => {
        const response = await api.put<Exam>(ENDPOINTS.EXAMS.BY_ID(id), data);
        return response.data;
    },

    /**
     * Obriši ispit
     */
    deleteExam: async (id: string): Promise<void> => {
        await api.delete(ENDPOINTS.EXAMS.BY_ID(id));
    },
};