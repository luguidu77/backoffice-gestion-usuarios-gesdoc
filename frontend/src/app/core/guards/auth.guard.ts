/**
 * Guard de autenticación (preparado para conectar con Alfresco).
 *
 * En Angular 17 los guards son funciones (CanActivateFn) en lugar de clases.
 *
 * Estado actual: permite el acceso a todas las rutas (modo desarrollo).
 *
 * Para implementar autenticación con Alfresco:
 *   1. Inyectar el servicio de autenticación: inject(AuthService)
 *   2. Verificar si existe un token JWT válido en localStorage
 *   3. Si no hay token → redirigir con Router a /login
 *   4. Si el token está expirado → intentar refresh token
 *   5. Si el refresh falla → redirigir a /login
 *
 * Uso en las rutas (app.routes.ts o dashboard.routes.ts):
 *   { path: 'dashboard', canActivate: [authGuard], ... }
 */
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
  // const router = inject(Router);
  // const authService = inject(AuthService);
  //
  // if (!authService.isLoggedIn()) {
  //   router.navigate(['/login']);
  //   return false;
  // }
  // return true;

  // TODO: Implementar autenticación con Alfresco
  return true;
};
