/**
 * Interceptor HTTP para añadir el ticket de autenticación.
 * 
 * Responsabilidades:
 *   - Añadir automáticamente el header Authorization a todas las peticiones HTTP
 *   - El ticket se obtiene del AuthService
 *   - Formato: Authorization: Basic {ticket}
 * 
 * Configuración:
 *   Este interceptor debe estar registrado en app.config.ts con provideHttpClient()
 */
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const ticket = authService.getTicket();

  // Si no hay ticket, continuar sin modificar la petición
  if (!ticket) {
    return next(req);
  }

  // Clonar la petición y añadir el header Authorization
  const authReq = req.clone({
    setHeaders: {
      Authorization: `Basic ${ticket}`
    }
  });

  return next(authReq);
};
