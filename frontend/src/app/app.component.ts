/**
 * Componente raíz de la aplicación.
 *
 * Es el único componente que se "bootstrapea" directamente (ver main.ts).
 * Actúa como contenedor del layout global:
 *   ┌──────────────────────────┐
 *   │  <app-header>            │  ← barra de navegación superior
 *   ├──────────────────────────┤
 *   │  <router-outlet>         │  ← aquí Angular renderiza la página activa
 *   ├──────────────────────────┤
 *   │  <app-footer>            │  ← pie de página
 *   └──────────────────────────┘
 *
 * standalone: true → no pertenece a ningún NgModule (Angular 17)
 * imports: [] → declara los componentes y directivas usadas en su template
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './layout/sidebar/sidebar.component';
import { FooterComponent } from './layout/footer/footer.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',       // Coincide con <app-root> en src/index.html
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'Backoffice DGGC';

  constructor(public authService: AuthService) { }
}
