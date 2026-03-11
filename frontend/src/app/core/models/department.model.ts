// Mapeado a Carpetas dentro del documentLibrary de Alfresco
export interface Department {
    id: string; // Node ID
    name: string; // Nombre de la carpeta
    parentId: string; // ID del nodo padre
    unitId: string; // Referencia al site/unit
    isInheritingPermissions: boolean;
    permissions?: AdvancedPermission[];
}

export interface AdvancedPermission {
    authorityId: string; // GROUP_... o userId
    authorityDisplayName?: string; // Nombre legible
    authorityType?: 'GROUP' | 'USER';
    name: string; // Coordinator | Collaborator | Contributor | Consumer | Editor
    accessStatus: 'ALLOWED' | 'DENIED';
}

export interface DepartmentListResponse {
    departments: Department[];
    totalItems: number;
    hasMore: boolean;
}

// ── Respuesta de permisos de un nodo ──────────────────────────────────────────

export interface NodePermissionsResponse {
    nodeId: string;
    nodeName: string;
    isInheritanceEnabled: boolean;
    locallySet: AdvancedPermission[];
    inherited: AdvancedPermission[];
}

/** Body para actualizar permisos de un nodo vía PUT /api/nodes/{nodeId}/permissions */
export interface UpdateNodePermissionsRequest {
    isInheritanceEnabled?: boolean;
    locallySet?: Pick<AdvancedPermission, 'authorityId' | 'name' | 'accessStatus'>[];
}

/** Roles de contenido disponibles en Alfresco para carpetas */
export const CONTENT_ROLES = [
    { value: 'Coordinator', label: 'Coordinador (Control Total)' },
    { value: 'Collaborator', label: 'Colaborador (Editar)' },
    { value: 'Contributor', label: 'Contribuidor (Crear)' },
    { value: 'Consumer', label: 'Lector (Solo Lectura)' },
    { value: 'Editor', label: 'Editor' },
] as const;

