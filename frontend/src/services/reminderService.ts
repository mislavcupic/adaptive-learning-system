import { apiClient } from '../api';
import type { StudentReminder, Task } from '../types';

export const reminderService = {
    getForStudent: async (): Promise<StudentReminder> => {
        return await apiClient.get<StudentReminder>('/reminders/student');
    },

    getPendingTasks: async (): Promise<Task[]> => {
        return await apiClient.get<Task[]>('/reminders/pending-tasks');
    },
};