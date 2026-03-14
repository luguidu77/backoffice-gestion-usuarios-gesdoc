export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  enabled: boolean;
  avatarId?: string;
  // Campos derivados
  memberOfUnits?: string[];
  groups?: string[];
  roles?: string[];
}

export interface UserListResponse {
  users: User[];
  totalUsers: number;
  hasMore: boolean;
}

export interface Group {
  id: string; // GROUP_...
  displayName: string;
  isRoot?: boolean;
}

export interface GroupListResponse {
  groups: Group[];
  totalItems: number;
}

export interface UserSiteMembership {
  siteId: string;
  siteTitle: string;
  role: string;
  visibility: string;
}

export interface UserSiteMembershipListResponse {
  sites: UserSiteMembership[];
  totalItems: number;
}

export type UnitReassignmentMode = 'TRANSFER' | 'ADD' | 'DEPARTMENTS';

export interface UnitReassignmentProofPayload {
  file: File;
  operationMode: UnitReassignmentMode;
  fromUnitIds: string[];
  targetUnitIds: string[];
  finalUnitIds: string[];
  transferFromUnitId?: string;
}

export interface UnitReassignmentProofResponse {
  userId: string;
  operationMode: UnitReassignmentMode;
  fromUnitIds: string[];
  targetUnitIds: string[];
  finalUnitIds: string[];
  originalFileName: string;
  storedFileName: string;
  storedPath: string;
  metadataPath: string;
  size: number;
  createdAt: string;
}

export interface UnitReassignmentAuditItem {
  fileName: string;
  nodeId: string;
  metadataNodeId?: string;
  folder: string;
  userId: string;
  operationMode?: string;
  createdAt?: string;
  metadata?: Record<string, unknown>;
}

export interface UnitReassignmentAuditListResponse {
  items: UnitReassignmentAuditItem[];
  totalItems: number;
  hasMore: boolean;
  skipCount: number;
  maxItems: number;
}
