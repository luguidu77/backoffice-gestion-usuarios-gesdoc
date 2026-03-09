/**
 * Componente de cabecera (navbar) de la aplicación.
 *
 * Muestra:
 *   - Logo / nombre de la aplicación (izquierda)
 *   - Navegación principal con enlaces de ruta (derecha)
 *
 * RouterLink: directiva de Angular Router para navegación SPA
 *   (sin recargar la página, a diferencia de <a href="">)
 *
 * RouterLinkActive: añade la clase CSS "active" al enlace
 *   correspondiente a la ruta activa actualmente
 */
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  appTitle = 'Backoffice DGGC';
}
