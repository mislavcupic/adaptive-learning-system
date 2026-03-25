import { apiClient } from '../api';
import type { StudentReminder, Task } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const reminderService = {
    getForStudent: async (): Promise<StudentReminder> => {
        const response = await apiClient.get<ApiResponse<StudentReminder>>('/reminders/student');
        return response.data;
    },

    getPendingTasks: async (): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>('/reminders/pending-tasks');
        return response.data;
    },
};
