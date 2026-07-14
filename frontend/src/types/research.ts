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
    warning: string | null;
}