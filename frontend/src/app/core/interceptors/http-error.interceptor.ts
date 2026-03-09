/**
 * Interceptor HTTP funcional para manejo centralizado de errores.
 *
 * Angular 17 recomienda interceptores funcionales (HttpInterceptorFn)
 * en lugar de clases que implementan HttpInterceptor.
 *
 * Este interceptor:
 *   1. Deja pasar todas las peticiones sin modificarlas
 *   2. Si la respuesta es un error, lo captura y lo loguea
 *   3. Propaga el error para que el componente/servicio pueda reaccionar
 *
 * Se registra en app.config.ts:
 *   provideHttpClient(withInterceptors([httpErrorInterceptor]))
 */
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      // Mensaje descriptivo según el tipo de error
      let mensaje: string;

      if (error.status === 0) {
        // Error de red: el servidor no responde o CORS bloqueado
        mensaje = 'Sin conexión con el servidor. ¿Está el backend arrancado en el puerto 8085?';
      } else if (error.status === 404) {
        mensaje = `Endpoint no encontrado: ${req.url}`;
      } else if (error.status >= 500) {
        mensaje = `Error interno del servidor (${error.status})`;
      } else {
        mensaje = `Error HTTP ${error.status}: ${error.message}`;
      }

      // Registrar en consola para depuración (solo en desarrollo)
      console.error(`[HTTP Error] ${mensaje}`, error);

      // Propagar el error original para que los componentes puedan manejarlo
      return throwError(() => error);
    })
  );
};
