// Modelos para la administración de grupos de Alfresco (GROUP_*)

export interface GroupItem {
    id: string;          // GROUP_xxx
    displayName: string;
    isRoot: boolean;
    memberCount?: number;
}

export interface GroupListResponse {
    groups: GroupItem[];
    totalItems: number;
    hasMore: boolean;
}

export interface GroupMemberItem {
    id: string;
    displayName: string;
    memberType: 'PERSON' | 'GROUP';
}

export interface GroupMembersResponse {
    groupId: string;
    groupDisplayName: string;
    members: GroupMemberItem[];
    totalItems: number;
}
