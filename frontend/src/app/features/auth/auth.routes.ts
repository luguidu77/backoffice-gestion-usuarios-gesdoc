import { Routes } from '@angular/router';
import { LoginPageComponent } from './pages/login-page/login-page.component';

/**
 * Rutas del módulo de autenticación.
 */
export const authRoutes: Routes = [
  {
    path: '',
    component: LoginPageComponent
  }
];
