export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  enabled: boolean;
}

export interface UserListResponse {
  users: User[];
  totalUsers: number;
  hasMore: boolean;
}
