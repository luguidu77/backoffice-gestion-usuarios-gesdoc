/**
 * Modelo para la petición de login.
 * Se envía al backend en POST /api/login
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Modelo para la respuesta de login del backend.
 * Contiene información del usuario autenticado y el ticket para futuras peticiones.
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
 * Modelo del usuario autenticado almacenado en sesión.
 */
export interface AuthUser {
  username: string;
  displayName: string;
  email: string;
  ticket: string;
}
