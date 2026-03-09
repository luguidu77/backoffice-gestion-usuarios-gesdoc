/**
 * Componente de cabecera (navbar) de la aplicación.
 *
 * Muestra:
 *   - Logo / nombre de la aplicación (izquierda)
 *   - Navegación principal con enlaces de ruta (centro)
 *   - Información del usuario y botón de logout (derecha)
 *
 * RouterLink: directiva de Angular Router para navegación SPA
 *   (sin recargar la página, a diferencia de <a href="">)
 *
 * RouterLinkActive: añade la clase CSS "active" al enlace
 *   correspondiente a la ruta activa actualmente
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  appTitle = 'Backoffice DGGC';

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  /**
   * Obtiene el nombre del usuario autenticado.
   */
  get userName(): string {
    const userData = this.authService.getUserData();
    if (userData) {
      return userData.firstName || userData.username || 'Usuario';
    }
    return 'Usuario';
  }

  /**
   * Cierra la sesión del usuario.
   */
  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        console.log('Sesión cerrada correctamente');
      },
      error: (error) => {
        console.error('Error al cerrar sesión:', error);
        // Incluso si hay error, redirigir al login
      },
      complete: () => {
        // La redirección al login ya se hace en authService.clearSession()
      }
    });
  }
}
