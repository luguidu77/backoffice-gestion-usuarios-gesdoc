/**
 * Rutas del feature Dashboard.
 *
 * Este fichero se carga de forma lazy desde app.routes.ts:
 *   loadChildren: () => import('./features/dashboard/dashboard.routes')
 *
 * Estructura de rutas del dashboard:
 *   /dashboard        → DashboardPageComponent (página principal)
 *
 * En el futuro se pueden añadir sub-rutas:
 *   /dashboard/usuarios  → UsuariosPageComponent
 *   /dashboard/informes  → InformesPageComponent
 */
import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    // Lazy load del componente de la página principal del dashboard
    loadComponent: () =>
      import('./pages/dashboard-page/dashboard-page.component').then(
        m => m.DashboardPageComponent
      )
  }
];
