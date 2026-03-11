// Mapeos de los roles internos de Alfresco para Sitios (Unidades)
export enum AlfrescoSiteRole {
    MANAGER = 'SiteManager',
    COLLABORATOR = 'SiteCollaborator',
    CONTRIBUTOR = 'SiteContributor',
    CONSUMER = 'SiteConsumer'
}

export interface Site {
    id: string; // Identifier en Alfresco
    title: string;
    description: string;
    visibility: string; // 'PUBLIC' | 'PRIVATE' | 'MODERATED'
    role?: AlfrescoSiteRole | string; // Rol del usuario actual
    managers?: string[];
}

export interface SiteListResponse {
    sites: Site[];
    totalSites: number;
    hasMore: boolean;
}

export interface UnitMember {
    id: string; // personId
    role: AlfrescoSiteRole | string;
    person: any;
}

// Alias de Unit = Site para usar nuestro lenguaje de negocio si se requiere
export type Unit = Site;
