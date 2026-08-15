import {SubmissionStatus} from "./index";

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



export interface GroupDescriptive {
    group: string;
    n: number;
    pretest_mean: number;
    pretest_sd: number;
    posttest_mean: number;
    posttest_sd: number;
}

export interface AncovaEffect {
    source: string;
    ss: number | null;
    df: number | null;
    f: number | null;
    p: number | null;
    partial_eta_sq: number | null;
}

export interface AncovaTableRow {
    source: string;
    ss: number | null;
    df: number | null;
    f: number | null;
    p: number | null;
    partial_eta_sq: number | null;
}

export interface AncovaResult {
    table: AncovaTableRow[];
    group_effect: AncovaEffect | null;
    significant: boolean;
}

export interface AncovaResponse {
    n_total: number;
    groups: string[];
    descriptives: GroupDescriptive[];
    ancova: AncovaResult | null;
    adjusted_means: AdjustedMean[] | null;
    effect_size: EffectSize | null;
    assumptions: Assumptions | null;
    warning: string | null;
}

export interface AdjustedMean {
    group: string;
    n: number;
    observed_mean: number;
    adjusted_mean: number;
    se: number | null;
}

export interface EffectSize {
    cohens_d: number;
    hedges_g: number;
    se: number;
    ci_lower: number;
    ci_upper: number;
    mean_difference: number;
    pooled_sd: number;
    magnitude: 'negligible' | 'small' | 'medium' | 'large';
    favors: string;
}

export interface AssumptionTest {
    f?: number | null;
    statistic?: number | null;
    p: number | null;
    satisfied: boolean;
    note?: string;
}

export interface Assumptions {
    homogeneity_of_slopes: AssumptionTest | null;
    levene: AssumptionTest | null;
    shapiro: AssumptionTest | null;
}


export interface AttemptEntry {
    submissionId: string;
    taskId: string;
    taskTitle: string;
    courseName: string;
    status: SubmissionStatus;
    aiScore: number | null;
    teacherScore: number | null;
    finalScore: number | null;
    maxScore: number | null;
    testsPassed: number | null;
    testsTotal: number | null;
    aiFeedback: string | null;
    teacherFeedback: string | null;
    executionTimeMs: number | null;
    createdAt: string;
}

export interface StudentBlock {
    studentId: string;
    firstName: string;
    lastName: string;
    email: string;
    schoolClassName: string | null;
    researchGroup: ResearchGroup;
    totalSubmissions: number;
    tasksAttempted: number;
    withAiFeedback: number;
    averageScore: number | null;
    lastActivity: string | null;
    attempts: AttemptEntry[];
}

export interface SubmissionsOverview {
    totalStudents: number;
    totalSubmissions: number;
    students: StudentBlock[];
}
