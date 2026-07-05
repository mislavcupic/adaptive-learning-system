export type ResearchGroup = 'NOT_ASSIGNED' | 'EXPERIMENTAL' | 'CONTROL';

export interface ResearchResult {
    studentId: string;
    firstName: string;
    lastName: string;
    email: string;
    researchGroup: ResearchGroup;
    pretestScore: number | null;
    pretestMaxScore: number | null;
    pretestPercentage: number | null;
    posttestScore: number | null;
    posttestMaxScore: number | null;
    posttestPercentage: number | null;
}