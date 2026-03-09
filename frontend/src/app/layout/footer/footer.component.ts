/**
 * Componente de pie de página.
 * Muestra el año actual calculado dinámicamente.
 */
import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  // El año se calcula en tiempo de ejecución para no tener que actualizarlo manualmente
  currentYear = new Date().getFullYear();
}
