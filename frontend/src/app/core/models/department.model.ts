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
    authorityId: string; // GROUP_...
    name: string; // Permiso
    accessStatus: 'ALLOWED' | 'DENIED';
}

export interface DepartmentListResponse {
    departments: Department[];
    totalItems: number;
    hasMore: boolean;
}
