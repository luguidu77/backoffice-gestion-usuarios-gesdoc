/**
 * Punto de entrada principal de la aplicación Angular 17.
 *
 * En Angular 17 con arquitectura standalone NO existe AppModule.
 * bootstrapApplication() arranca directamente el componente raíz
 * con la configuración definida en app.config.ts.
 *
 * Flujo de arranque:
 *   1. El navegador carga index.html
 *   2. index.html carga este main.ts (compilado a main.js)
 *   3. bootstrapApplication arranca AppComponent
 *   4. Angular procesa <app-root> en el HTML y renderiza la app
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error('Error al arrancar la aplicación Angular:', err));
