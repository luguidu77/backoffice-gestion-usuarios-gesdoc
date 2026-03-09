/**
 * Configuración principal de la aplicación Angular 17 (arquitectura standalone).
 *
 * En Angular 17 standalone NO existe AppModule.
 * ApplicationConfig reemplaza al NgModule como punto centralizado
 * donde se registran todos los providers globales.
 *
 * Providers registrados aquí están disponibles en TODA la aplicación.
 */
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Router: carga las rutas definidas en app.routes.ts
    // withComponentInputBinding() permite pasar parámetros de ruta como @Input()
    provideRouter(routes, withComponentInputBinding()),

    // HttpClient global: disponible en todos los servicios vía inyección de dependencias
    // withInterceptors() registra los interceptores funcionales de Angular 17
    // Orden: primero authInterceptor (añade ticket), luego httpErrorInterceptor (maneja errores)
    provideHttpClient(
      withInterceptors([authInterceptor, httpErrorInterceptor])
    ),

    // Habilita las animaciones de Angular (necesarias para algunos componentes UI)
    provideAnimations()
  ]
};
