export interface Site {
    id: string;
    title: string;
    description: string;
    visibility: string;
}

export interface SiteListResponse {
    sites: Site[];
    totalSites: number;
    hasMore: boolean;
}
