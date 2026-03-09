import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';

/**
 * Modelo de respuesta del login
 */
export interface LoginResponse {
  success: boolean;
  ticket?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  message?: string;
}

/**
 * Modelo de credenciales de login
 */
export interface LoginCredentials {
  username: string;
  password: string;
}

/**
 * Servicio de autenticación.
 * 
 * Responsabilidades:
 *   - Gestionar el login/logout de usuarios
 *   - Almacenar y recuperar el ticket de autenticación
 *   - Proporcionar información sobre el estado de autenticación
 *   - Validar tickets
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api/auth';
  private readonly STORAGE_KEY = 'auth_ticket';
  private readonly USER_KEY = 'auth_user';

  // Observable para el estado de autenticación
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidTicket());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Inicia sesión con las credenciales proporcionadas.
   * 
   * @param credentials Usuario y contraseña
   * @returns Observable con la respuesta del login
   */
  login(credentials: LoginCredentials): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        if (response.success && response.ticket) {
          // Guardar ticket y datos de usuario en localStorage
          this.saveTicket(response.ticket);
          this.saveUserData(response);
          this.isAuthenticatedSubject.next(true);
        }
      })
    );
  }

  /**
   * Cierra la sesión del usuario actual.
   * 
   * @returns Observable con la respuesta del logout
   */
  logout(): Observable<any> {
    const ticket = this.getTicket();
    
    if (!ticket) {
      this.clearSession();
      return new Observable(observer => {
        observer.next({ success: true });
        observer.complete();
      });
    }

    return this.http.post(`${this.API_URL}/logout`, { ticket }).pipe(
      tap(() => {
        this.clearSession();
      })
    );
  }

  /**
   * Valida si un ticket es válido.
   * 
   * @param ticket Ticket a validar
   * @returns Observable con el resultado de la validación
   */
  validateTicket(ticket: string): Observable<any> {
    return this.http.get(`${this.API_URL}/validate`, {
      params: { ticket }
    });
  }

  /**
   * Verifica si el usuario está autenticado.
   * 
   * @returns true si hay un ticket válido almacenado
   */
  isAuthenticated(): boolean {
    return this.hasValidTicket();
  }

  /**
   * Obtiene el ticket de autenticación almacenado.
   * 
   * @returns Ticket o null si no existe
   */
  getTicket(): string | null {
    return localStorage.getItem(this.STORAGE_KEY);
  }

  /**
   * Obtiene los datos del usuario autenticado.
   * 
   * @returns Datos del usuario o null si no existe
   */
  getUserData(): any {
    const userData = localStorage.getItem(this.USER_KEY);
    return userData ? JSON.parse(userData) : null;
  }

  /**
   * Guarda el ticket en localStorage.
   */
  private saveTicket(ticket: string): void {
    localStorage.setItem(this.STORAGE_KEY, ticket);
  }

  /**
   * Guarda los datos del usuario en localStorage.
   */
  private saveUserData(data: LoginResponse): void {
    const userData = {
      username: data.username,
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
  }

  /**
   * Verifica si existe un ticket válido en localStorage.
   */
  private hasValidTicket(): boolean {
    const ticket = this.getTicket();
    return ticket !== null && ticket.trim().length > 0;
  }

  /**
   * Limpia la sesión del usuario.
   */
  private clearSession(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }
}
