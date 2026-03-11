/**
 * Modelo para la petición de login.
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Modelo para la respuesta de login del backend.
 */
export interface LoginResponse {
  success: boolean;
  username?: string;
  displayName?: string;
  email?: string;
  ticket?: string;
  message: string;
}

/**
 * Roles de aplicación (nuestro front-end).
 */
export enum AppRole {
  GLOBAL_ADMIN = 'GLOBAL_ADMIN',
  UNIT_ADMIN = 'UNIT_ADMIN',
  READ_ONLY = 'READ_ONLY'
}

/**
 * Modelo del usuario autenticado almacenado en sesión, con permisos caculados.
 */
export interface AuthUser {
  username: string;
  displayName: string;
  email: string;
  ticket: string;
  // Campos derivados para permisos
  id?: string;
  role?: AppRole;
  managedSiteIds?: string[];
  groups?: string[];
  isGlobalAdmin?: boolean;
}

export interface SessionScope {
  userId: string;
  isGlobalAdmin: boolean;
  managedSiteIds: string[]; // Lista de IDs de sitios (Unidades) donde es SiteManager
  groups: string[]; // Lista de IDs de grupos
  role: AppRole; // Rol derivado de la lógica de negocio
}
