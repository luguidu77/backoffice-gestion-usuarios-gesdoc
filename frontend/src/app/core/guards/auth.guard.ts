/**
 * Guard de autenticación con Alfresco.
 *
 * En Angular 17 los guards son funciones (CanActivateFn) en lugar de clases.
 *
 * Responsabilidades:
 *   - Verificar si el usuario está autenticado antes de acceder a rutas protegidas
 *   - Redirigir a /login si no hay autenticación válida
 *   - Preservar la URL destino para redireccionar después del login
 *
 * Uso en las rutas (app.routes.ts o dashboard.routes.ts):
 *   { path: 'dashboard', canActivate: [authGuard], ... }
 */
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Verificar si el usuario está autenticado
  if (authService.isAuthenticated()) {
    return true;
  }

  // Si no está autenticado, redirigir al login
  console.warn('Usuario no autenticado. Redirigiendo a /login');
  
  // Guardar la URL a la que intentaba acceder para redirigir después del login
  router.navigate(['/login'], {
    queryParams: { returnUrl: state.url }
  });
  
  return false;
};
