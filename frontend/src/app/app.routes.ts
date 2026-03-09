/**
 * Rutas principales de la aplicación (nivel raíz).
 *
 * Arquitectura de rutas con lazy loading:
 *   /           → redirige a /dashboard
 *   /login      → página de autenticación
 *   /dashboard  → carga el módulo de dashboard bajo demanda (lazy) - protegido por authGuard
 *   /users      → gestión de usuarios de Alfresco - protegido por authGuard
 *   /**         → cualquier ruta desconocida redirige al dashboard
 *
 * Lazy loading (loadChildren / loadComponent):
 *   El código de cada feature solo se descarga cuando el usuario
 *   navega a esa ruta → bundles más pequeños → inicio más rápido.
 */
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    // Ruta de login - acceso público
    path: 'login',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.authRoutes)
  },
  {
    // Carga lazy del feature Dashboard - protegida con authGuard
    // Angular 17: se usa loadChildren con las rutas del feature
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    // Gestión de usuarios - protegida con authGuard
    path: 'users',
    loadChildren: () =>
      import('./features/users/users.routes').then(m => m.usersRoutes)
  },
  {
    // Wildcard: ruta no encontrada → redirige al dashboard
    path: '**',
    redirectTo: 'dashboard'
  }
];
