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
