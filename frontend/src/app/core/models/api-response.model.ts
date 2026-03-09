/**
 * Modelos de datos para las respuestas de la API.
 *
 * Centralizar los tipos TypeScript aquí:
 *   - Evita duplicar definiciones en distintos componentes
 *   - Facilita el refactoring cuando cambia la API
 *   - Proporciona autocompletado e intellisense en el IDE
 */

/**
 * Envoltorio genérico para cualquier respuesta de la API.
 * El backend puede devolver { data: T, message: string }
 */
export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
}

/**
 * Estado de salud del servidor backend.
 * Usado por el endpoint /ping (simplificado como string).
 */
export interface HealthStatus {
  status: 'UP' | 'DOWN';
  timestamp: string;
}

/**
 * Información de un usuario (placeholder para gestión de usuarios).
 * Se ampliará cuando se conecte con Alfresco.
 */
export interface Usuario {
  id: string;
  nombre: string;
  apellidos: string;
  email: string;
  activo: boolean;
}
