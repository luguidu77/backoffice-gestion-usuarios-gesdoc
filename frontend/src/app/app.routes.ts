/**
 * Rutas principales de la aplicación (nivel raíz).
 *
 * Arquitectura de rutas con lazy loading:
 *   /           → redirige a /dashboard
 *   /dashboard  → carga el módulo de dashboard bajo demanda (lazy)
 *   /**         → cualquier ruta desconocida redirige al dashboard
 *
 * Lazy loading (loadChildren / loadComponent):
 *   El código de cada feature solo se descarga cuando el usuario
 *   navega a esa ruta → bundles más pequeños → inicio más rápido.
 */
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    // Carga lazy del feature Dashboard
    // Angular 17: se usa loadChildren con las rutas del feature
    path: 'dashboard',
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    // Wildcard: ruta no encontrada → redirige al dashboard
    path: '**',
    redirectTo: 'dashboard'
  }
];
